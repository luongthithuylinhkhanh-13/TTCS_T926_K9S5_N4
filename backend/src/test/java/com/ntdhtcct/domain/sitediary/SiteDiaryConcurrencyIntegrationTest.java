package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SiteDiaryConcurrencyIntegrationTest {

    @Mock
    private SiteDiaryRepository siteDiaryRepository;

    @Mock
    private SiteDiaryLockRepository siteDiaryLockRepository;

    @Test
    @DisplayName("Concurrency: Thao tác khóa sổ phải đợi giao dịch ghi đang giữ shared lock kết thúc; giao dịch ghi đến sau phải bị từ chối")
    void testLockBlocksUntilOngoingSyncCompletes_andSubsequentSyncRejected() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.of(2026, 10, 15);

        // Khởi tạo advisory lock manager không có PostgreSQL -> dùng JVM fallback ReentrantReadWriteLock
        SiteDiaryAdvisoryLockManager lockManager = new SiteDiaryAdvisoryLockManager(null);
        SiteDiaryLockService lockService = new SiteDiaryLockService(siteDiaryLockRepository, lockManager);
        SiteDiarySyncService syncService = new SiteDiarySyncService(siteDiaryRepository, lockService, lockManager);

        AtomicBoolean isLocked = new AtomicBoolean(false);
        when(siteDiaryLockRepository.existsByProjectIdAndLockDateAndStatus(projectId, targetDate, "LOCKED"))
                .thenAnswer(inv -> isLocked.get());

        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, targetDate))
                .thenAnswer(inv -> Optional.empty());

        when(siteDiaryLockRepository.save(any(SiteDiaryLock.class))).thenAnswer(inv -> {
            SiteDiaryLock saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            isLocked.set("LOCKED".equalsIgnoreCase(saved.getStatus()));
            return saved;
        });

        when(siteDiaryRepository.findByClientSyncId(any())).thenReturn(Optional.empty());
        when(siteDiaryRepository.save(any(SiteDiary.class))).thenAnswer(inv -> {
            SiteDiary diary = inv.getArgument(0);
            diary.setId(UUID.randomUUID());
            return diary;
        });

        CountDownLatch sync1StartedHoldingLock = new CountDownLatch(1);
        CountDownLatch lockAttemptStarted = new CountDownLatch(1);
        CountDownLatch sync1CanFinish = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // Luồng 1: Giao dịch ghi 1 (sync 1) bắt đầu trước và giữ shared lock
            Future<SiteDiarySyncResult> sync1Future = executor.submit(() -> {
                // Ta bao bọc bằng việc giữ shared lock giống như syncSingleDiary làm
                lockManager.acquireSharedLock(projectId, targetDate);
                try {
                    sync1StartedHoldingLock.countDown();
                    // Đợi luồng khóa cố gắng lock
                    sync1CanFinish.await(5, TimeUnit.SECONDS);

                    // Hoàn thành ghi
                    SiteDiarySyncRequest req1 = new SiteDiarySyncRequest(
                            "sync-concurrent-1", targetDate, "Nắng", "30", 10, "", "CV 1", "", OffsetDateTime.now()
                    );
                    return syncService.syncSingleDiary(projectId, userId, req1);
                } finally {
                    lockManager.releaseSharedLock(projectId, targetDate);
                }
            });

            // Chờ luồng 1 chắc chắn đã giữ shared lock
            assertThat(sync1StartedHoldingLock.await(3, TimeUnit.SECONDS)).isTrue();

            // Luồng 2: Project Manager thực hiện khóa sổ ngày targetDate (yêu cầu Exclusive Lock)
            AtomicBoolean lockCompleted = new AtomicBoolean(false);
            Future<?> lockFuture = executor.submit(() -> {
                lockAttemptStarted.countDown();
                lockService.lockDate(projectId, userId, targetDate, "Khóa ngày");
                lockCompleted.set(true);
            });

            // Chờ luồng 2 bắt đầu thực thi
            assertThat(lockAttemptStarted.await(2, TimeUnit.SECONDS)).isTrue();
            // Đợi một chút để chứng minh luồng 2 BỊ BLOCKED (chưa xong vì luồng 1 đang giữ shared lock)
            Thread.sleep(150);
            assertThat(lockCompleted.get())
                    .as("Thao tác khóa sổ phải chờ luồng ghi 1 giải phóng Shared Lock")
                    .isFalse();

            // Cho phép luồng 1 hoàn tất
            sync1CanFinish.countDown();
            SiteDiarySyncResult sync1Result = sync1Future.get(5, TimeUnit.SECONDS);
            assertThat(sync1Result.getStatus()).isEqualTo("CREATED");

            // Chờ luồng 2 hoàn tất sau khi luồng 1 đã giải phóng lock
            lockFuture.get(5, TimeUnit.SECONDS);
            assertThat(lockCompleted.get()).isTrue();
            assertThat(isLocked.get()).isTrue();

            // Luồng 3: Giao dịch ghi đến sau khi khóa sổ đã thành công
            SiteDiarySyncRequest req3 = new SiteDiarySyncRequest(
                    "sync-subsequent-3", targetDate, "Mưa", "25", 10, "", "CV 3", "", OffsetDateTime.now()
            );
            SiteDiarySyncResult sync3Result = syncService.syncSingleDiary(projectId, userId, req3);

            // Kiểm tra kết quả đồng bộ thực tế: Giao dịch ghi đến sau BẮT BUỘC BỊ TỪ CHỐI
            assertThat(sync3Result.getStatus()).isEqualTo("LOCKED");
            assertThat(sync3Result.getErrorCode()).isEqualTo("DIARY_DATE_LOCKED");
            assertThat(sync3Result.getMessage()).contains("đã bị khóa sổ");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("H2 Fallback Lifecycle: Khóa ReentrantReadWriteLock không bị release khi service method kết thúc mà được giữ đến transaction completion")
    void testH2LockHeldUntilTransactionCompletion_NotReleasedPrematurelyInFinally() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.of(2026, 10, 16);

        SiteDiaryAdvisoryLockManager lockManager = new SiteDiaryAdvisoryLockManager(null);
        SiteDiaryLockService lockService = new SiteDiaryLockService(siteDiaryLockRepository, lockManager);
        SiteDiarySyncService syncService = new SiteDiarySyncService(siteDiaryRepository, lockService, lockManager);

        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, targetDate))
                .thenReturn(Optional.empty());
        when(siteDiaryLockRepository.save(any(SiteDiaryLock.class))).thenAnswer(inv -> inv.getArgument(0));

        when(siteDiaryRepository.findByClientSyncId(any())).thenReturn(Optional.empty());
        when(siteDiaryRepository.save(any(SiteDiary.class))).thenAnswer(inv -> {
            SiteDiary d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        CountDownLatch syncMethodFinished = new CountDownLatch(1);
        CountDownLatch lockAttemptStarted = new CountDownLatch(1);
        CountDownLatch transactionCanComplete = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Luồng 1: Giả lập Spring Transaction interceptor quản lý TransactionSynchronization
            Future<SiteDiarySyncResult> syncTxFuture = executor.submit(() -> {
                // Kích hoạt transaction synchronization giả lập vòng đời của Spring @Transactional
                org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
                org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
                try {
                    SiteDiarySyncRequest req = new SiteDiarySyncRequest(
                            "sync-tx-test", targetDate, "Nắng", "29", 10, "", "CV", "", OffsetDateTime.now()
                    );

                    // syncSingleDiary thực hiện: acquireSharedLock -> xử lý -> finally { releaseSharedLock }
                    // Nhờ TransactionSynchronization, releaseSharedLock trong finally không được release ngay
                    SiteDiarySyncResult res = syncService.syncSingleDiary(projectId, userId, req);

                    // Báo hiệu service method đã return (finally đã chạy xong)
                    syncMethodFinished.countDown();

                    // Transaction CHƯA commit: giữ synchronization active và chờ
                    transactionCanComplete.await(5, TimeUnit.SECONDS);

                    return res;
                } finally {
                    // Giả lập commit: chạy afterCompletion trên tất cả synchronizations đã đăng ký
                    for (org.springframework.transaction.support.TransactionSynchronization sync :
                            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) {
                        sync.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
                    }
                    org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
                    org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
                }
            });

            // Chờ luồng 1 hoàn tất syncSingleDiary (service method đã return)
            assertThat(syncMethodFinished.await(3, TimeUnit.SECONDS)).isTrue();

            // Luồng 2: Cố gắng lấy exclusive lock để khóa sổ
            AtomicBoolean lockCompleted = new AtomicBoolean(false);
            Future<?> lockFuture = executor.submit(() -> {
                lockAttemptStarted.countDown();
                // Phương thức này đòi hỏi Exclusive Lock
                lockService.lockDate(projectId, userId, targetDate, "Khóa ngày sau giao dịch ghi");
                lockCompleted.set(true);
            });

            assertThat(lockAttemptStarted.await(2, TimeUnit.SECONDS)).isTrue();
            // Đợi một khoảng thời gian để chứng minh luồng 2 VẪN BỊ CHẶN dù syncSingleDiary đã return
            Thread.sleep(150);
            assertThat(lockCompleted.get())
                    .as("Khóa ReentrantReadWriteLock PHẢI được giữ đến khi transaction commit, không được nhả ngay khi method kết thúc")
                    .isFalse();

            // Cho phép transaction của luồng 1 hoàn tất commit (afterCompletion được kích hoạt)
            transactionCanComplete.countDown();
            syncTxFuture.get(5, TimeUnit.SECONDS);

            // Bây giờ luồng 2 mới có thể lấy exclusive lock và hoàn tất
            lockFuture.get(5, TimeUnit.SECONDS);
            assertThat(lockCompleted.get()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
