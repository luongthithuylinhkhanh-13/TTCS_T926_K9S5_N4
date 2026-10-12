package com.ntdhtcct.domain.sitediary;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Quản lý đồng bộ concurrency cho khóa sổ nhật ký công trường theo cặp (projectId, date).
 * 
 * Trên PostgreSQL:
 * Sử dụng transaction-level advisory locks:
 * - pg_advisory_xact_lock_shared(:key) cho thao tác GHI (sync, create, update).
 * - pg_advisory_xact_lock(:key) cho thao tác KHÓA / MỞ KHÓA (exclusive).
 * Lock tự động giữ suốt transaction và tự động giải phóng khi transaction commit hoặc rollback.
 * 
 * Trên môi trường không phải PostgreSQL (ví dụ H2 trong test):
 * Tự động fallback sang JVM-level ReentrantReadWriteLock phân cấp theo lockKey để đảm bảo
 * kiểm thử đa luồng vẫn tái hiện và kiểm chứng chính xác ngữ nghĩa concurrency.
 */
@Component
public class SiteDiaryAdvisoryLockManager {

    private static final Logger log = LoggerFactory.getLogger(SiteDiaryAdvisoryLockManager.class);

    private final EntityManager entityManager;
    private final ConcurrentHashMap<Long, ReentrantReadWriteLock> jvmLocks = new ConcurrentHashMap<>();
    private volatile Boolean isPostgreSql = null;

    public SiteDiaryAdvisoryLockManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Sinh khóa số nguyên 64-bit bất biến và phân bố đều từ cặp (projectId, date).
     */
    public long generateLockKey(UUID projectId, LocalDate date) {
        if (projectId == null || date == null) {
            return 0L;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] input = (projectId.toString() + ":" + date.toString()).getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(input);
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            long high = projectId.getMostSignificantBits() ^ projectId.getLeastSignificantBits();
            return high * 31 + date.toEpochDay();
        }
    }

    /**
     * Lấy Shared Lock cho thao tác ghi nhật ký (create, update, sync).
     * Cho phép nhiều luồng ghi cùng ngày chạy song song, nhưng chặn và bị chặn bởi Exclusive Lock của thao tác Khóa sổ.
     */
    public void acquireSharedLock(UUID projectId, LocalDate date) {
        if (projectId == null || date == null) return;
        long key = generateLockKey(projectId, date);
        if (isPostgreSqlDatabase()) {
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock_shared(:key)")
                    .setParameter("key", key)
                    .getSingleResult();
        } else {
            ReentrantReadWriteLock rwLock = jvmLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock(true));
            rwLock.readLock().lock();
        }
    }

    /**
     * Lấy Exclusive Lock cho thao tác Khóa hoặc Mở khóa sổ.
     * Chờ tất cả các giao dịch ghi đang dở dang kết thúc, và đóng băng mọi giao dịch ghi đến sau.
     */
    public void acquireExclusiveLock(UUID projectId, LocalDate date) {
        if (projectId == null || date == null) return;
        long key = generateLockKey(projectId, date);
        if (isPostgreSqlDatabase()) {
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                    .setParameter("key", key)
                    .getSingleResult();
        } else {
            ReentrantReadWriteLock rwLock = jvmLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock(true));
            rwLock.writeLock().lock();
        }
    }

    /**
     * Giải phóng lock thủ công (chỉ áp dụng cho H2 / non-PostgreSQL JVM fallback).
     * Trên PostgreSQL, Advisory Xact Lock tự động nhả khi commit/rollback nên không cần gọi giải phóng.
     */
    public void releaseSharedLock(UUID projectId, LocalDate date) {
        if (projectId == null || date == null) return;
        if (!isPostgreSqlDatabase()) {
            long key = generateLockKey(projectId, date);
            ReentrantReadWriteLock rwLock = jvmLocks.get(key);
            if (rwLock != null && rwLock.getReadHoldCount() > 0) {
                rwLock.readLock().unlock();
            }
        }
    }

    public void releaseExclusiveLock(UUID projectId, LocalDate date) {
        if (projectId == null || date == null) return;
        if (!isPostgreSqlDatabase()) {
            long key = generateLockKey(projectId, date);
            ReentrantReadWriteLock rwLock = jvmLocks.get(key);
            if (rwLock != null && rwLock.isWriteLockedByCurrentThread()) {
                rwLock.writeLock().unlock();
            }
        }
    }

    private boolean isPostgreSqlDatabase() {
        if (isPostgreSql == null) {
            synchronized (this) {
                if (isPostgreSql == null) {
                    try {
                        String dialect = (String) entityManager.getEntityManagerFactory()
                                .getProperties().get("hibernate.dialect");
                        if (dialect != null && dialect.toLowerCase().contains("postgresql")) {
                            isPostgreSql = true;
                        } else {
                            // Thử kiểm tra qua database engine name
                            Object result = entityManager.createNativeQuery("SELECT version()").getSingleResult();
                            isPostgreSql = result != null && result.toString().toLowerCase().contains("postgresql");
                        }
                    } catch (Exception e) {
                        isPostgreSql = false;
                    }
                }
            }
        }
        return isPostgreSql;
    }
}
