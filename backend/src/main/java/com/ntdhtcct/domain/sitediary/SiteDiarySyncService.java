package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý đồng bộ ngoại tuyến nhật ký công trường (S-30: NTDHTCT-214).
 * Hỗ trợ các nhiệm vụ:
 *   - NTDHTCT-255 (T-70): Xử lý dữ liệu từ hàng đợi chưa đồng bộ.
 *   - NTDHTCT-256 (T-71): Tiếp nhận và xử lý dữ liệu tự động gửi lên máy chủ khi có mạng trở lại.
 * Đảm bảo Idempotency (chống trùng lặp dữ liệu) thông qua clientSyncId.
 */
@Service
public class SiteDiarySyncService {

    private final SiteDiaryRepository siteDiaryRepository;

    public SiteDiarySyncService(SiteDiaryRepository siteDiaryRepository) {
        this.siteDiaryRepository = siteDiaryRepository;
    }

    /**
     * Đồng bộ một bản ghi nhật ký công trường từ hàng đợi ngoại tuyến.
     * Nếu bản ghi với clientSyncId đã tồn tại trên server -> trả về ALREADY_EXISTS (Idempotent).
     */
    @Transactional
    public SiteDiarySyncResult syncSingleDiary(UUID projectId, UUID reporterId, SiteDiarySyncRequest request) {
        if (request.getClientSyncId() != null && !request.getClientSyncId().isBlank()) {
            Optional<SiteDiary> existingOpt = siteDiaryRepository.findByClientSyncId(request.getClientSyncId());
            if (existingOpt.isPresent()) {
                return SiteDiarySyncResult.alreadyExists(request.getClientSyncId(), existingOpt.get().getId());
            }
        }

        try {
            OffsetDateTime offlineTime = request.getOfflineCreatedAt() != null
                    ? request.getOfflineCreatedAt()
                    : OffsetDateTime.now();

            SiteDiary diary = new SiteDiary(
                    projectId,
                    reporterId,
                    request.getDiaryDate(),
                    request.getWeather(),
                    request.getTemperature(),
                    request.getWorkerCount(),
                    request.getEquipmentStatus(),
                    request.getWorkSummary(),
                    request.getIssues(),
                    request.getClientSyncId(),
                    offlineTime
            );

            SiteDiary saved = siteDiaryRepository.save(diary);
            return SiteDiarySyncResult.created(request.getClientSyncId(), saved.getId());
        } catch (Exception e) {
            return SiteDiarySyncResult.error(request.getClientSyncId(), e.getMessage());
        }
    }

    /**
     * Đồng bộ hàng loạt (Batch Sync) các bản ghi từ hàng đợi ngoại tuyến khi thiết bị có mạng trở lại.
     */
    @Transactional
    public SiteDiaryBatchSyncResponse syncBatchDiaries(UUID projectId, UUID reporterId, SiteDiaryBatchSyncRequest batchRequest) {
        List<SiteDiarySyncResult> results = new ArrayList<>();
        int createdCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        for (SiteDiarySyncRequest item : batchRequest.getItems()) {
            SiteDiarySyncResult result = syncSingleDiary(projectId, reporterId, item);
            results.add(result);

            if ("CREATED".equals(result.getStatus())) {
                createdCount++;
            } else if ("ALREADY_EXISTS".equals(result.getStatus())) {
                duplicateCount++;
            } else {
                failedCount++;
            }
        }

        return new SiteDiaryBatchSyncResponse(
                batchRequest.getItems().size(),
                createdCount,
                duplicateCount,
                failedCount,
                results
        );
    }

    /**
     * Lấy danh sách nhật ký công trường của một dự án.
     */
    @Transactional(readOnly = true)
    public List<SiteDiaryResponse> getDiariesByProject(UUID projectId) {
        return siteDiaryRepository.findByProjectIdOrderByDiaryDateDesc(projectId)
                .stream()
                .map(SiteDiaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết nhật ký công trường theo ID.
     */
    @Transactional(readOnly = true)
    public Optional<SiteDiaryResponse> getDiaryById(UUID projectId, UUID diaryId) {
        return siteDiaryRepository.findById(Objects.requireNonNull(diaryId))
                .filter(d -> d.getProjectId().equals(projectId))
                .map(SiteDiaryResponse::from);
    }

    /**
     * Cập nhật nội dung nhật ký, chỉ khi bản ghi thuộc đúng dự án.
     */
    @Transactional
    public Optional<SiteDiaryResponse> updateDiary(
            UUID projectId,
            UUID diaryId,
            SiteDiaryUpdateRequest request
    ) {
        return siteDiaryRepository.findById(Objects.requireNonNull(diaryId))
                .filter(diary -> diary.getProjectId().equals(projectId))
                .map(diary -> {
                    diary.setDiaryDate(request.diaryDate());
                    diary.setWeather(request.weather());
                    diary.setTemperature(request.temperature());
                    diary.setWorkerCount(request.workerCount());
                    diary.setEquipmentStatus(request.equipmentStatus());
                    diary.setWorkSummary(request.workSummary());
                    diary.setIssues(request.issues());
                    return siteDiaryRepository.save(diary);
                })
                .map(SiteDiaryResponse::from);
    }
}
