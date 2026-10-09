package com.ntdhtcct.domain.sitediary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SiteDiarySyncIntegrationTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SiteDiarySyncService siteDiarySyncService;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private SiteDiaryController siteDiaryController;

    private final UUID projectId = UUID.randomUUID();
    private final UUID engineerUserId = UUID.randomUUID();
    private final String token = "valid-test-token-site-engineer";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(siteDiaryController).build();
    }

    @Test
    @DisplayName("TC-SYNC-01: Đồng bộ thành công 1 bản ghi từ hàng đợi ngoại tuyến (T-70, T-71)")
    void testSyncSingleDiary_Success() throws Exception {
        String clientSyncId = "sync-item-" + UUID.randomUUID();
        OffsetDateTime offlineCreatedAt = OffsetDateTime.now().minusHours(2);
        UUID savedDiaryId = UUID.randomUUID();

        SiteDiarySyncRequest request = new SiteDiarySyncRequest(
                clientSyncId,
                LocalDate.now(),
                "Nắng nhẹ, 31 độ C",
                "31°C",
                45,
                "2 máy xúc, 1 cẩu tháp, 3 xe trộn bê tông",
                "Đổ bê tông sàn tầng 5 khu A, bảo dưỡng cốt thép dầm",
                "Không có phát sinh hay tai nạn lao động",
                offlineCreatedAt
        );

        when(siteDiarySyncService.syncSingleDiary(eq(projectId), any(), any(SiteDiarySyncRequest.class)))
                .thenReturn(SiteDiarySyncResult.created(clientSyncId, savedDiaryId));

        mockMvc.perform(post("/api/projects/{projectId}/site-diaries/sync", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", engineerUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSyncId", equalTo(clientSyncId)))
                .andExpect(jsonPath("$.status", equalTo("CREATED")))
                .andExpect(jsonPath("$.serverDiaryId", equalTo(savedDiaryId.toString())));
    }

    @Test
    @DisplayName("TC-SYNC-02: Cơ chế Idempotent - Gửi lại bản ghi có cùng clientSyncId không bị nhân bản (T-71)")
    void testSyncSingleDiary_IdempotentOnDuplicate() throws Exception {
        String clientSyncId = "sync-duplicate-check-" + UUID.randomUUID();
        UUID existingDiaryId = UUID.randomUUID();

        SiteDiarySyncRequest request = new SiteDiarySyncRequest(
                clientSyncId,
                LocalDate.now(),
                "Mưa rào buổi chiều",
                "28°C",
                30,
                "1 cẩu tháp dừng thi công lúc 14h do mưa",
                "Lắp đặt cốt thép cột tầng 6",
                "Mưa lớn gián đoạn 2 giờ",
                OffsetDateTime.now().minusHours(4)
        );

        when(siteDiarySyncService.syncSingleDiary(eq(projectId), any(), any(SiteDiarySyncRequest.class)))
                .thenReturn(SiteDiarySyncResult.alreadyExists(clientSyncId, existingDiaryId));

        mockMvc.perform(post("/api/projects/{projectId}/site-diaries/sync", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", engineerUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSyncId", equalTo(clientSyncId)))
                .andExpect(jsonPath("$.status", equalTo("ALREADY_EXISTS")))
                .andExpect(jsonPath("$.serverDiaryId", equalTo(existingDiaryId.toString())));
    }

    @Test
    @DisplayName("TC-SYNC-03: Tự động gửi hàng loạt dữ liệu (Batch Sync) khi có mạng trở lại (T-71)")
    void testBatchSync_WhenOnlineRestored() throws Exception {
        String syncId1 = "sync-batch-1-" + UUID.randomUUID();
        String syncId2 = "sync-batch-2-" + UUID.randomUUID();
        UUID diaryId1 = UUID.randomUUID();
        UUID diaryId2 = UUID.randomUUID();

        SiteDiarySyncRequest item1 = new SiteDiarySyncRequest(
                syncId1,
                LocalDate.now().minusDays(1),
                "Nắng",
                "32°C",
                40,
                "Máy đào",
                "Đào đất móng trụ T1",
                "Bình thường",
                OffsetDateTime.now().minusDays(1)
        );

        SiteDiarySyncRequest item2 = new SiteDiarySyncRequest(
                syncId2,
                LocalDate.now(),
                "Mây rải rác",
                "30°C",
                42,
                "Máy ủi, cẩu",
                "Đổ bê tông lót móng trụ T1",
                "Bình thường",
                OffsetDateTime.now()
        );

        SiteDiaryBatchSyncRequest batchRequest = new SiteDiaryBatchSyncRequest(List.of(item1, item2));

        SiteDiaryBatchSyncResponse batchResponse = new SiteDiaryBatchSyncResponse(
                2,
                2,
                0,
                0,
                List.of(
                        SiteDiarySyncResult.created(syncId1, diaryId1),
                        SiteDiarySyncResult.created(syncId2, diaryId2)
                )
        );

        when(siteDiarySyncService.syncBatchDiaries(eq(projectId), any(), any(SiteDiaryBatchSyncRequest.class)))
                .thenReturn(batchResponse);

        mockMvc.perform(post("/api/projects/{projectId}/site-diaries/batch-sync", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", engineerUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed", equalTo(2)))
                .andExpect(jsonPath("$.createdCount", equalTo(2)))
                .andExpect(jsonPath("$.duplicateCount", equalTo(0)))
                .andExpect(jsonPath("$.failedCount", equalTo(0)));
    }

    @Test
    @DisplayName("TC-SYNC-04: Lấy danh sách nhật ký công trường của dự án sau khi đồng bộ thành công")
    void testGetDiariesByProject_AfterSync() throws Exception {
        String clientSyncId = "sync-list-test-" + UUID.randomUUID();

        SiteDiary diary = new SiteDiary(
                projectId,
                engineerUserId,
                LocalDate.now(),
                "Nắng gắt",
                "35°C",
                50,
                "Toàn bộ máy hoạt động tốt",
                "Gia công cốt thép dầm sàn",
                "Không có",
                clientSyncId,
                OffsetDateTime.now()
        );
        SiteDiaryResponse response = SiteDiaryResponse.from(diary);

        when(siteDiarySyncService.getDiariesByProject(projectId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/projects/{projectId}/site-diaries", projectId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", engineerUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workSummary", equalTo("Gia công cốt thép dầm sàn")))
                .andExpect(jsonPath("$[0].clientSyncId", equalTo(clientSyncId)));
    }
}
