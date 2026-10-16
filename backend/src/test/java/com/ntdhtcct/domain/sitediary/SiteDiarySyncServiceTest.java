package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class SiteDiarySyncServiceTest {

    @Mock
    private SiteDiaryRepository siteDiaryRepository;

    private SiteDiarySyncService siteDiarySyncService;

    private final UUID projectId = UUID.randomUUID();
    private final UUID reporterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        siteDiarySyncService = new SiteDiarySyncService(siteDiaryRepository);
    }

    @Test
    @DisplayName("T-70 & T-71: Đồng bộ bản ghi mới khi chưa có trong hệ thống")
    void syncSingleDiary_NewRecord_ShouldCreate() {
        String syncId = "client-sync-001";
        SiteDiarySyncRequest req = new SiteDiarySyncRequest(
                syncId,
                LocalDate.now(),
                "Nắng",
                "30°C",
                20,
                "Máy đào",
                "Đào rãnh thoát nước",
                "Không",
                OffsetDateTime.now()
        );
        req.setEngineerCount(2);
        req.setCrewCount(3);
        req.setCrewDetails("Tổ cốt thép: 8 người");
        req.setWorkingConditions("Mưa làm mặt bằng trơn");

        when(siteDiaryRepository.findByClientSyncId(syncId)).thenReturn(Optional.empty());
        when(siteDiaryRepository.save(any(SiteDiary.class))).thenAnswer(invocation -> {
            SiteDiary diary = invocation.getArgument(0);
            diary.setId(UUID.randomUUID());
            return diary;
        });

        SiteDiarySyncResult result = siteDiarySyncService.syncSingleDiary(projectId, reporterId, req);

        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getClientSyncId()).isEqualTo(syncId);
        ArgumentCaptor<SiteDiary> diaryCaptor = ArgumentCaptor.forClass(SiteDiary.class);
        verify(siteDiaryRepository).save(diaryCaptor.capture());
        SiteDiary savedDiary = diaryCaptor.getValue();
        assertThat(result.getServerDiaryId()).isEqualTo(savedDiary.getId());
        assertThat(savedDiary.getEngineerCount()).isEqualTo(2);
        assertThat(savedDiary.getCrewCount()).isEqualTo(3);
        assertThat(savedDiary.getCrewDetails()).isEqualTo("Tổ cốt thép: 8 người");
        assertThat(savedDiary.getWorkingConditions()).isEqualTo("Mưa làm mặt bằng trơn");
    }

    @Test
    @DisplayName("T-71: Xử lý Idempotency - Không tạo thêm khi clientSyncId đã tồn tại")
    void syncSingleDiary_DuplicateRecord_ShouldReturnAlreadyExists() {
        String syncId = "client-sync-duplicate";
        SiteDiarySyncRequest req = new SiteDiarySyncRequest(
                syncId,
                LocalDate.now(),
                "Mưa",
                "25°C",
                15,
                "Không",
                "Trú mưa",
                "Mưa lớn",
                OffsetDateTime.now()
        );

        SiteDiary existing = new SiteDiary();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setClientSyncId(syncId);

        when(siteDiaryRepository.findByClientSyncId(syncId)).thenReturn(Optional.of(existing));

        SiteDiarySyncResult result = siteDiarySyncService.syncSingleDiary(projectId, reporterId, req);

        assertThat(result.getStatus()).isEqualTo("ALREADY_EXISTS");
        assertThat(result.getClientSyncId()).isEqualTo(syncId);
        assertThat(result.getServerDiaryId()).isEqualTo(existingId);
        verify(siteDiaryRepository, never()).save(any(SiteDiary.class));
    }

    @Test
    @DisplayName("T-71: Batch Sync xử lý tổng hợp các bản ghi thành công và trùng lặp")
    void syncBatchDiaries_MixedRecords_ShouldReportAccurately() {
        String newSyncId = "sync-new";
        String dupSyncId = "sync-dup";

        SiteDiarySyncRequest req1 = new SiteDiarySyncRequest(newSyncId, LocalDate.now(), "Nắng", "30", 10, "", "CV 1", "", OffsetDateTime.now());
        SiteDiarySyncRequest req2 = new SiteDiarySyncRequest(dupSyncId, LocalDate.now(), "Mưa", "28", 12, "", "CV 2", "", OffsetDateTime.now());

        when(siteDiaryRepository.findByClientSyncId(newSyncId)).thenReturn(Optional.empty());

        SiteDiary existing = new SiteDiary();
        existing.setId(UUID.randomUUID());
        when(siteDiaryRepository.findByClientSyncId(dupSyncId)).thenReturn(Optional.of(existing));

        SiteDiary saved = new SiteDiary();
        saved.setId(UUID.randomUUID());
        when(siteDiaryRepository.save(any(SiteDiary.class))).thenReturn(saved);

        SiteDiaryBatchSyncResponse batchResponse = siteDiarySyncService.syncBatchDiaries(
                projectId,
                reporterId,
                new SiteDiaryBatchSyncRequest(List.of(req1, req2))
        );

        assertThat(batchResponse.getTotalProcessed()).isEqualTo(2);
        assertThat(batchResponse.getCreatedCount()).isEqualTo(1);
        assertThat(batchResponse.getDuplicateCount()).isEqualTo(1);
        assertThat(batchResponse.getFailedCount()).isEqualTo(0);
    }
}
