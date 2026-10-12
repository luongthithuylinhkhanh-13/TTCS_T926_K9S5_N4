package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.sitediary.dto.SiteDiaryLockResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý trạng thái khóa sổ nhật ký công trường theo ngày (S-24: T-64).
 */
@Service
public class SiteDiaryLockService {

    private final SiteDiaryLockRepository siteDiaryLockRepository;

    public SiteDiaryLockService(SiteDiaryLockRepository siteDiaryLockRepository) {
        this.siteDiaryLockRepository = siteDiaryLockRepository;
    }

    /**
     * Khóa sổ nhật ký của dự án theo ngày.
     * Đảm bảo tính Idempotent: nếu ngày đó đã ở trạng thái LOCKED thì giữ nguyên và trả về.
     */
    @Transactional
    public SiteDiaryLockResponse lockDate(UUID projectId, UUID userId, LocalDate lockDate, String reason) {
        Objects.requireNonNull(projectId, "projectId không được để trống");
        Objects.requireNonNull(lockDate, "lockDate không được để trống");

        SiteDiaryLock lock = siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate)
                .map(existing -> {
                    if ("LOCKED".equalsIgnoreCase(existing.getStatus())) {
                        return existing;
                    }
                    existing.setStatus("LOCKED");
                    existing.setLockedBy(userId);
                    existing.setLockedAt(OffsetDateTime.now());
                    existing.setLockReason(reason);
                    return siteDiaryLockRepository.save(existing);
                })
                .orElseGet(() -> {
                    SiteDiaryLock newLock = new SiteDiaryLock(projectId, lockDate, userId, reason);
                    return siteDiaryLockRepository.save(newLock);
                });

        return SiteDiaryLockResponse.from(lock);
    }

    /**
     * Mở khóa sổ nhật ký của ngày đã khóa.
     */
    @Transactional
    public SiteDiaryLockResponse unlockDate(UUID projectId, UUID userId, LocalDate lockDate, String reason) {
        Objects.requireNonNull(projectId, "projectId không được để trống");
        Objects.requireNonNull(lockDate, "lockDate không được để trống");

        SiteDiaryLock lock = siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate)
                .map(existing -> {
                    existing.setStatus("UNLOCKED");
                    existing.setUnlockedBy(userId);
                    existing.setUnlockedAt(OffsetDateTime.now());
                    existing.setUnlockReason(reason);
                    return siteDiaryLockRepository.save(existing);
                })
                .orElseGet(() -> {
                    // Nếu chưa từng có bản ghi thì tạo trạng thái UNLOCKED
                    SiteDiaryLock newLock = new SiteDiaryLock(projectId, lockDate, userId, null);
                    newLock.setStatus("UNLOCKED");
                    newLock.setUnlockedBy(userId);
                    newLock.setUnlockedAt(OffsetDateTime.now());
                    newLock.setUnlockReason(reason);
                    return siteDiaryLockRepository.save(newLock);
                });

        return SiteDiaryLockResponse.from(lock);
    }

    /**
     * Kiểm tra xem ngày cụ thể trong dự án có đang bị khóa sổ không.
     */
    @Transactional(readOnly = true)
    public boolean isDateLocked(UUID projectId, LocalDate lockDate) {
        if (projectId == null || lockDate == null) {
            return false;
        }
        return siteDiaryLockRepository.existsByProjectIdAndLockDateAndStatus(projectId, lockDate, "LOCKED");
    }

    /**
     * Lấy trạng thái khóa sổ của một ngày cụ thể trong dự án.
     */
    @Transactional(readOnly = true)
    public Optional<SiteDiaryLockResponse> getLockStatus(UUID projectId, LocalDate lockDate) {
        return siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate)
                .map(SiteDiaryLockResponse::from);
    }

    /**
     * Lấy danh sách tất cả các ngày đã thiết lập khóa/mở khóa sổ của dự án.
     */
    @Transactional(readOnly = true)
    public List<SiteDiaryLockResponse> getProjectLocks(UUID projectId) {
        return siteDiaryLockRepository.findByProjectIdOrderByLockDateDesc(projectId)
                .stream()
                .map(SiteDiaryLockResponse::from)
                .collect(Collectors.toList());
    }
}
