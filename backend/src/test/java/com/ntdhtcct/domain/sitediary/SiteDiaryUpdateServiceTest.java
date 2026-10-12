package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.common.exception.DiaryDateLockedException;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteDiaryUpdateServiceTest {

    @Mock
    private SiteDiaryRepository siteDiaryRepository;

    @Mock
    private SiteDiaryLockService siteDiaryLockService;

    @Test
    void updatesDiaryContentWithoutChangingItsProjectOrReporter() {
        UUID projectId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID diaryId = UUID.randomUUID();
        SiteDiary diary = new SiteDiary(
                projectId,
                reporterId,
                LocalDate.of(2026, 10, 8),
                "Nắng",
                "32°C",
                30,
                "Máy xúc",
                "Đào móng",
                null,
                "client-sync-1",
                OffsetDateTime.now()
        );
        diary.setId(diaryId);
        SiteDiaryUpdateRequest request = new SiteDiaryUpdateRequest(
                LocalDate.of(2026, 10, 9),
                "Mưa nhẹ",
                "27°C",
                2,
                24,
                3,
                "Tổ cốt thép: 8 người; tổ cốp pha: 6 người",
                "Máy bơm",
                "Mưa làm mặt bằng trơn",
                "Đổ bê tông móng",
                "Tạm dừng do mưa"
        );
        when(siteDiaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
        when(siteDiaryRepository.save(diary)).thenReturn(diary);
        when(siteDiaryLockService.isDateLocked(projectId, LocalDate.of(2026, 10, 8))).thenReturn(false);
        when(siteDiaryLockService.isDateLocked(projectId, LocalDate.of(2026, 10, 9))).thenReturn(false);

        Optional<?> result = new SiteDiarySyncService(siteDiaryRepository, siteDiaryLockService)
                .updateDiary(projectId, diaryId, request);

        assertThat(result).isPresent();
        assertThat(diary.getProjectId()).isEqualTo(projectId);
        assertThat(diary.getReporterId()).isEqualTo(reporterId);
        assertThat(diary.getDiaryDate()).isEqualTo(request.diaryDate());
        assertThat(diary.getWorkSummary()).isEqualTo(request.workSummary());
        assertThat(diary.getWeather()).isEqualTo(request.weather());
        assertThat(diary.getTemperature()).isEqualTo(request.temperature());
        assertThat(diary.getEngineerCount()).isEqualTo(request.engineerCount());
        assertThat(diary.getWorkerCount()).isEqualTo(request.workerCount());
        assertThat(diary.getCrewCount()).isEqualTo(request.crewCount());
        assertThat(diary.getCrewDetails()).isEqualTo(request.crewDetails());
        assertThat(diary.getEquipmentStatus()).isEqualTo(request.equipmentStatus());
        assertThat(diary.getWorkingConditions()).isEqualTo(request.workingConditions());
        assertThat(diary.getIssues()).isEqualTo(request.issues());
        assertThat(diary.getClientSyncId()).isEqualTo("client-sync-1");
        verify(siteDiaryRepository).save(diary);
    }

    @Test
    void doesNotUpdateDiaryFromAnotherProject() {
        UUID diaryId = UUID.randomUUID();
        SiteDiary diary = new SiteDiary();
        diary.setProjectId(UUID.randomUUID());
        when(siteDiaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));

        Optional<?> result = new SiteDiarySyncService(siteDiaryRepository, siteDiaryLockService).updateDiary(
                UUID.randomUUID(),
                diaryId,
                new SiteDiaryUpdateRequest(
                        LocalDate.now(), null, null, null, null, null, null, null, null, "Updated", null
                )
        );

        assertThat(result).isEmpty();
        verify(siteDiaryRepository, never()).save(any(SiteDiary.class));
    }

    @Test
    @DisplayName("T-65: Ném DiaryDateLockedException khi ngày gốc của nhật ký đã bị khóa")
    void throwsExceptionWhenOriginalDateIsLocked() {
        UUID projectId = UUID.randomUUID();
        UUID diaryId = UUID.randomUUID();
        LocalDate originalDate = LocalDate.of(2026, 10, 5);

        SiteDiary diary = new SiteDiary();
        diary.setId(diaryId);
        diary.setProjectId(projectId);
        diary.setDiaryDate(originalDate);

        when(siteDiaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
        when(siteDiaryLockService.isDateLocked(projectId, originalDate)).thenReturn(true);

        SiteDiaryUpdateRequest request = new SiteDiaryUpdateRequest(
                originalDate, "Nắng", "30°C", 1, 20, 2, "Tổ 1", "Xe tải", "Bình thường", "Nội dung", "Không"
        );

        SiteDiarySyncService service = new SiteDiarySyncService(siteDiaryRepository, siteDiaryLockService);

        assertThatThrownBy(() -> service.updateDiary(projectId, diaryId, request))
                .isInstanceOf(DiaryDateLockedException.class)
                .hasMessageContaining("đã bị khóa sổ");

        verify(siteDiaryRepository, never()).save(any(SiteDiary.class));
    }

    @Test
    @DisplayName("T-65: Ném DiaryDateLockedException khi chuyển nhật ký sang một ngày mới đã bị khóa")
    void throwsExceptionWhenTargetDateIsLocked() {
        UUID projectId = UUID.randomUUID();
        UUID diaryId = UUID.randomUUID();
        LocalDate originalDate = LocalDate.of(2026, 10, 5);
        LocalDate targetLockedDate = LocalDate.of(2026, 10, 6);

        SiteDiary diary = new SiteDiary();
        diary.setId(diaryId);
        diary.setProjectId(projectId);
        diary.setDiaryDate(originalDate);

        when(siteDiaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
        when(siteDiaryLockService.isDateLocked(projectId, originalDate)).thenReturn(false);
        when(siteDiaryLockService.isDateLocked(projectId, targetLockedDate)).thenReturn(true);

        SiteDiaryUpdateRequest request = new SiteDiaryUpdateRequest(
                targetLockedDate, "Nắng", "30°C", 1, 20, 2, "Tổ 1", "Xe tải", "Bình thường", "Nội dung", "Không"
        );

        SiteDiarySyncService service = new SiteDiarySyncService(siteDiaryRepository, siteDiaryLockService);

        assertThatThrownBy(() -> service.updateDiary(projectId, diaryId, request))
                .isInstanceOf(DiaryDateLockedException.class)
                .hasMessageContaining("Ngày mới")
                .hasMessageContaining("đã bị khóa sổ");

        verify(siteDiaryRepository, never()).save(any(SiteDiary.class));
    }
}
