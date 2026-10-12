package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.common.exception.DiaryDateLockedException;
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
 * Service xử lý đồng bộ ngoại tuyến nhật ký công trường (S-30: NTDHTCT-214)
 * và kiểm soát khóa sổ (S-24: T-64, T-65).
 */
@Service
public class SiteDiarySyncService {

    private final SiteDiaryRepository siteDiaryRepository;
    private final SiteDiaryLockService siteDiaryLockService;

    public SiteDiarySyncService(
            SiteDiaryRepository siteDiaryRepository,
            SiteDiaryLockService siteDiaryLockService) {
        this.siteDiaryRepository = siteDiaryRepository;
        this.siteDiaryLockService = siteDiaryLockService;
    }

    /**
     * Đồng bộ một bản ghi nhật ký công trường từ hàng đợi ngoại tuyến hoặc tạo trực tiếp.
     * T-65: Nếu ngày của nhật ký đã bị khóa sổ, từ chối tạo mới.
     */
    @Transactional
    public SiteDiarySyncResult syncSingleDiary(UUID projectId, UUID reporterId, SiteDiarySyncRequest request) {
        if (siteDiaryLockService.isDateLocked(projectId, request.getDiaryDate())) {
            return SiteDiarySyncResult.locked(
                    request.getClientSyncId(),
                    "Nhật ký công trường ngày " + request.getDiaryDate() + " đã bị khóa sổ. Thao tác bị từ chối."
            );
        }

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
            diary.setEngineerCount(request.getEngineerCount());
            diary.setCrewCount(request.getCrewCount());
            diary.setCrewDetails(request.getCrewDetails());
            diary.setWorkingConditions(request.getWorkingConditions());

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
     * T-65: Kiểm tra cả ngày gốc của nhật ký và ngày mới yêu cầu.
     * Nếu một trong hai ngày đã bị khóa sổ -> ném DiaryDateLockedException.
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
                    // 1. Kiểm tra ngày gốc của bản ghi
                    if (siteDiaryLockService.isDateLocked(projectId, diary.getDiaryDate())) {
                        throw new DiaryDateLockedException(
                                "Nhật ký công trường ngày " + diary.getDiaryDate() + " đã bị khóa sổ. Không thể chỉnh sửa."
                        );
                    }

                    // 2. Nếu thay đổi sang ngày khác, kiểm tra ngày mới
                    if (request.diaryDate() != null && !request.diaryDate().equals(diary.getDiaryDate())) {
                        if (siteDiaryLockService.isDateLocked(projectId, request.diaryDate())) {
                            throw new DiaryDateLockedException(
                                    "Ngày mới " + request.diaryDate() + " đã bị khóa sổ. Không thể chuyển nhật ký sang ngày này."
                            );
                        }
                    }

                    diary.setDiaryDate(request.diaryDate());
                    diary.setWeather(request.weather());
                    diary.setTemperature(request.temperature());
                    diary.setEngineerCount(request.engineerCount());
                    diary.setWorkerCount(request.workerCount());
                    diary.setCrewCount(request.crewCount());
                    diary.setCrewDetails(request.crewDetails());
                    diary.setEquipmentStatus(request.equipmentStatus());
                    diary.setWorkingConditions(request.workingConditions());
                    diary.setWorkSummary(request.workSummary());
                    diary.setIssues(request.issues());
                    return siteDiaryRepository.save(diary);
                })
                .map(SiteDiaryResponse::from);
    }
}
