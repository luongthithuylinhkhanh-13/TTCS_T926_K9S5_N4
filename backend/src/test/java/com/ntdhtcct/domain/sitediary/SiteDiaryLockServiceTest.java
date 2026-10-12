package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.sitediary.dto.SiteDiaryLockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteDiaryLockServiceTest {

    @Mock
    private SiteDiaryLockRepository siteDiaryLockRepository;

    @InjectMocks
    private SiteDiaryLockService siteDiaryLockService;

    private UUID projectId;
    private UUID userId;
    private LocalDate lockDate;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        lockDate = LocalDate.now();
    }

    @Test
    @DisplayName("Khóa thành công ngày chưa có bản ghi khóa")
    void testLockDate_WhenNewDate_CreatesLockedRecord() {
        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate))
                .thenReturn(Optional.empty());

        when(siteDiaryLockRepository.save(any(SiteDiaryLock.class)))
                .thenAnswer(invocation -> {
                    SiteDiaryLock lock = invocation.getArgument(0);
                    lock.setId(UUID.randomUUID());
                    return lock;
                });

        SiteDiaryLockResponse response = siteDiaryLockService.lockDate(
                projectId,
                userId,
                lockDate,
                "Chốt khối lượng ngày"
        );

        assertThat(response.locked()).isTrue();
        assertThat(response.status()).isEqualTo("LOCKED");
        assertThat(response.lockReason()).isEqualTo("Chốt khối lượng ngày");
        assertThat(response.lockedBy()).isEqualTo(userId);
        verify(siteDiaryLockRepository).save(any(SiteDiaryLock.class));
    }

    @Test
    @DisplayName("Khóa sổ idempotent: Nếu ngày đã LOCKED thì trả về bản ghi hiện tại, không save lại")
    void testLockDate_WhenAlreadyLocked_ReturnsExisting() {
        SiteDiaryLock existing = new SiteDiaryLock(projectId, lockDate, userId, "Đã khóa từ trước");
        existing.setId(UUID.randomUUID());
        existing.setStatus("LOCKED");

        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate))
                .thenReturn(Optional.of(existing));

        SiteDiaryLockResponse response = siteDiaryLockService.lockDate(
                projectId,
                userId,
                lockDate,
                "Khóa lại"
        );

        assertThat(response.locked()).isTrue();
        assertThat(response.status()).isEqualTo("LOCKED");
        verify(siteDiaryLockRepository, never()).save(any(SiteDiaryLock.class));
    }

    @Test
    @DisplayName("Khóa sổ khi bản ghi trước đó ở trạng thái UNLOCKED thì chuyển sang LOCKED")
    void testLockDate_WhenPreviouslyUnlocked_UpdatesToLocked() {
        SiteDiaryLock existing = new SiteDiaryLock(projectId, lockDate, userId, "Lý do cũ");
        existing.setId(UUID.randomUUID());
        existing.setStatus("UNLOCKED");

        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate))
                .thenReturn(Optional.of(existing));

        when(siteDiaryLockRepository.save(any(SiteDiaryLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SiteDiaryLockResponse response = siteDiaryLockService.lockDate(
                projectId,
                userId,
                lockDate,
                "Khóa sổ sau khi bổ sung"
        );

        assertThat(response.locked()).isTrue();
        assertThat(response.status()).isEqualTo("LOCKED");
        assertThat(response.lockReason()).isEqualTo("Khóa sổ sau khi bổ sung");
        verify(siteDiaryLockRepository).save(existing);
    }

    @Test
    @DisplayName("Mở khóa sổ ngày đã khóa")
    void testUnlockDate_WhenLocked_UpdatesToUnlocked() {
        SiteDiaryLock existing = new SiteDiaryLock(projectId, lockDate, userId, "Khóa ngày");
        existing.setId(UUID.randomUUID());
        existing.setStatus("LOCKED");

        when(siteDiaryLockRepository.findByProjectIdAndLockDate(projectId, lockDate))
                .thenReturn(Optional.of(existing));

        when(siteDiaryLockRepository.save(any(SiteDiaryLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SiteDiaryLockResponse response = siteDiaryLockService.unlockDate(
                projectId,
                userId,
                lockDate,
                "Mở khóa để bổ sung quân số"
        );

        assertThat(response.locked()).isFalse();
        assertThat(response.status()).isEqualTo("UNLOCKED");
        assertThat(response.unlockReason()).isEqualTo("Mở khóa để bổ sung quân số");
        assertThat(response.unlockedBy()).isEqualTo(userId);
        verify(siteDiaryLockRepository).save(existing);
    }

    @Test
    @DisplayName("Kiểm tra trạng thái isDateLocked")
    void testIsDateLocked() {
        when(siteDiaryLockRepository.existsByProjectIdAndLockDateAndStatus(projectId, lockDate, "LOCKED"))
                .thenReturn(true);

        assertThat(siteDiaryLockService.isDateLocked(projectId, lockDate)).isTrue();

        LocalDate otherDate = lockDate.plusDays(1);
        when(siteDiaryLockRepository.existsByProjectIdAndLockDateAndStatus(projectId, otherDate, "LOCKED"))
                .thenReturn(false);

        assertThat(siteDiaryLockService.isDateLocked(projectId, otherDate)).isFalse();
    }

    @Test
    @DisplayName("Lấy danh sách các ngày đã thiết lập khóa sổ của dự án")
    void testGetProjectLocks() {
        SiteDiaryLock lock1 = new SiteDiaryLock(projectId, lockDate, userId, "Khóa ngày 1");
        lock1.setId(UUID.randomUUID());
        when(siteDiaryLockRepository.findByProjectIdOrderByLockDateDesc(projectId))
                .thenReturn(List.of(lock1));

        List<SiteDiaryLockResponse> list = siteDiaryLockService.getProjectLocks(projectId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).lockReason()).isEqualTo("Khóa ngày 1");
    }
}
