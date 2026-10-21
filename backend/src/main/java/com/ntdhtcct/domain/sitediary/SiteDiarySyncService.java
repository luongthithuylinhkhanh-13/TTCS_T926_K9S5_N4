package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.common.exception.DiaryDateLockedException;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncStatusResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUpdateRequest;
import com.ntdhtcct.domain.diary.DiaryEntry;
import com.ntdhtcct.domain.diary.DiaryEntryRepository;
import com.ntdhtcct.domain.diary.DiaryImage;
import com.ntdhtcct.domain.diary.DiaryImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final int STRIPE_COUNT = 256;

    private final SiteDiaryRepository siteDiaryRepository;
    private final SiteDiaryLockService siteDiaryLockService;
    private final SiteDiaryAdvisoryLockManager advisoryLockManager;
    private final DiaryEntryRepository diaryEntryRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final Object[] bridgeStripes = new Object[STRIPE_COUNT];
    private final TransactionOperations requiresNewTransactionOperations;

    @Autowired
    public SiteDiarySyncService(
            SiteDiaryRepository siteDiaryRepository,
            SiteDiaryLockService siteDiaryLockService,
            SiteDiaryAdvisoryLockManager advisoryLockManager,
            @Autowired(required = false) DiaryEntryRepository diaryEntryRepository,
            @Autowired(required = false) DiaryImageRepository diaryImageRepository,
            @Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.siteDiaryRepository = siteDiaryRepository;
        this.siteDiaryLockService = siteDiaryLockService;
        this.advisoryLockManager = advisoryLockManager;
        this.diaryEntryRepository = diaryEntryRepository;
        this.diaryImageRepository = diaryImageRepository;

        for (int i = 0; i < STRIPE_COUNT; i++) {
            this.bridgeStripes[i] = new Object();
        }

        if (transactionManager != null) {
            this.requiresNewTransactionOperations = new TransactionTemplate(
                    transactionManager,
                    new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
            );
        } else {
            this.requiresNewTransactionOperations = TransactionOperations.withoutTransaction();
        }
    }

    public SiteDiarySyncService(
            SiteDiaryRepository siteDiaryRepository,
            SiteDiaryLockService siteDiaryLockService,
            SiteDiaryAdvisoryLockManager advisoryLockManager,
            DiaryEntryRepository diaryEntryRepository,
            PlatformTransactionManager transactionManager) {
        this(siteDiaryRepository, siteDiaryLockService, advisoryLockManager, diaryEntryRepository, null, transactionManager);
    }

    public SiteDiarySyncService(
            SiteDiaryRepository siteDiaryRepository,
            SiteDiaryLockService siteDiaryLockService,
            SiteDiaryAdvisoryLockManager advisoryLockManager,
            DiaryEntryRepository diaryEntryRepository) {
        this(siteDiaryRepository, siteDiaryLockService, advisoryLockManager, diaryEntryRepository, null, null);
    }

    public SiteDiarySyncService(
            SiteDiaryRepository siteDiaryRepository,
            SiteDiaryLockService siteDiaryLockService,
            SiteDiaryAdvisoryLockManager advisoryLockManager) {
        this(siteDiaryRepository, siteDiaryLockService, advisoryLockManager, null, null, null);
    }

    /**
     * Đồng bộ một bản ghi nhật ký công trường từ hàng đợi ngoại tuyến hoặc tạo trực tiếp.
     * T-65: Nếu ngày của nhật ký đã bị khóa sổ, từ chối tạo mới.
     * Giữ Shared Advisory Lock trong suốt transaction để đảm bảo thứ tự nghiêm ngặt với thao tác Khóa sổ.
     */
    @Transactional
    public SiteDiarySyncResult syncSingleDiary(UUID projectId, UUID reporterId, SiteDiarySyncRequest request) {
        advisoryLockManager.acquireSharedLock(projectId, request.getDiaryDate());
        try {
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
                syncDiaryEntryBridge(saved);
                return SiteDiarySyncResult.created(request.getClientSyncId(), saved.getId());
            } catch (Exception e) {
                return SiteDiarySyncResult.error(request.getClientSyncId(), e.getMessage());
            }
        } finally {
            advisoryLockManager.releaseSharedLock(projectId, request.getDiaryDate());
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
     * Lấy Shared Advisory Lock cho cả ngày cũ và ngày mới (theo thứ tự ngày tăng dần để tránh deadlock).
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
                    LocalDate oldDate = diary.getDiaryDate();
                    LocalDate newDate = request.diaryDate();

                    // Xác định danh sách ngày cần lock theo thứ tự tăng dần (tránh deadlock)
                    List<LocalDate> datesToLock = new ArrayList<>();
                    if (oldDate != null) datesToLock.add(oldDate);
                    if (newDate != null && !newDate.equals(oldDate)) datesToLock.add(newDate);
                    datesToLock.sort(Comparator.naturalOrder());

                    for (LocalDate d : datesToLock) {
                        advisoryLockManager.acquireSharedLock(projectId, d);
                    }

                    try {
                        // 1. Kiểm tra ngày gốc của bản ghi
                        if (siteDiaryLockService.isDateLocked(projectId, oldDate)) {
                            throw new DiaryDateLockedException(
                                    "Nhật ký công trường ngày " + oldDate + " đã bị khóa sổ. Không thể chỉnh sửa."
                            );
                        }

                        // 2. Nếu thay đổi sang ngày khác, kiểm tra ngày mới
                        if (newDate != null && !newDate.equals(oldDate)) {
                            if (siteDiaryLockService.isDateLocked(projectId, newDate)) {
                                throw new DiaryDateLockedException(
                                        "Ngày mới " + newDate + " đã bị khóa sổ. Không thể chuyển nhật ký sang ngày này."
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
                        SiteDiary saved = siteDiaryRepository.save(diary);
                        syncDiaryEntryBridge(saved);
                        return saved;
                    } finally {
                        for (LocalDate d : datesToLock) {
                            advisoryLockManager.releaseSharedLock(projectId, d);
                        }
                    }
                })
                .map(SiteDiaryResponse::from);
    }

    /**
     * Xóa nhật ký công trường, chỉ khi bản ghi thuộc đúng dự án.
     * S-24: Kiểm tra ngày của nhật ký đã bị khóa sổ hay chưa.
     * Nếu ngày đã bị khóa -> ném DiaryDateLockedException.
     * Giữ Shared Advisory Lock trong quá trình xóa.
     */
    @Transactional
    public boolean deleteDiary(UUID projectId, UUID diaryId) {
        if (diaryId == null || projectId == null) {
            return false;
        }

        Optional<SiteDiary> diaryOpt = siteDiaryRepository.findById(diaryId)
                .filter(d -> d.getProjectId().equals(projectId));
        if (diaryOpt.isEmpty()) {
            return false;
        }

        SiteDiary diary = diaryOpt.get();
        LocalDate diaryDate = diary.getDiaryDate();

        if (diaryDate != null) {
            advisoryLockManager.acquireSharedLock(projectId, diaryDate);
        }

        try {
            if (diaryDate != null && siteDiaryLockService.isDateLocked(projectId, diaryDate)) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + diaryDate + " đã bị khóa sổ. Không thể xóa."
                );
            }

            if (diaryImageRepository != null) {
                List<DiaryImage> images = diaryImageRepository.findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(diaryId);
                if (images != null && !images.isEmpty()) {
                    diaryImageRepository.deleteAll(images);
                }
            }

            if (diaryEntryRepository != null && diaryEntryRepository.existsById(diaryId)) {
                diaryEntryRepository.deleteById(diaryId);
            }

            siteDiaryRepository.delete(diary);
            return true;
        } finally {
            if (diaryDate != null) {
                advisoryLockManager.releaseSharedLock(projectId, diaryDate);
            }
        }
    }

    private Object getBridgeLock(UUID entryId) {
        if (entryId == null) {
            return bridgeStripes[0];
        }
        int index = (entryId.hashCode() & 0x7fffffff) % STRIPE_COUNT;
        return bridgeStripes[index];
    }

    private static String toSafeTitle(String workSummary) {
        if (workSummary == null || workSummary.isBlank()) {
            return "Nhật ký thi công";
        }
        String trimmed = workSummary.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }

    private static String toSafeWeather(String weather) {
        if (weather == null) {
            return null;
        }
        String trimmed = weather.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }

    private void updateEntryFields(DiaryEntry entry, SiteDiary sd) {
        entry.setEntryDate(sd.getDiaryDate());
        entry.setWeather(toSafeWeather(sd.getWeather()));
        entry.setTitle(toSafeTitle(sd.getWorkSummary()));
        entry.setContent(sd.getWorkSummary());
    }

    private void syncDiaryEntryBridge(SiteDiary sd) {
        if (diaryEntryRepository == null || sd == null || sd.getId() == null) {
            return;
        }
        try {
            Optional<DiaryEntry> existing = diaryEntryRepository.findById(sd.getId());
            if (existing.isPresent()) {
                DiaryEntry entry = existing.get();
                updateEntryFields(entry, sd);
                diaryEntryRepository.save(entry);
                return;
            }

            Object lock = getBridgeLock(sd.getId());
            synchronized (lock) {
                existing = diaryEntryRepository.findById(sd.getId());
                if (existing.isPresent()) {
                    DiaryEntry entry = existing.get();
                    updateEntryFields(entry, sd);
                    diaryEntryRepository.save(entry);
                    return;
                }

                try {
                    requiresNewTransactionOperations.execute(status -> {
                        Optional<DiaryEntry> inTx = diaryEntryRepository.findById(sd.getId());
                        if (inTx.isPresent()) {
                            DiaryEntry entry = inTx.get();
                            updateEntryFields(entry, sd);
                            return diaryEntryRepository.save(entry);
                        }
                        DiaryEntry bridged = new DiaryEntry(
                                sd.getProjectId(),
                                sd.getReporterId() != null ? sd.getReporterId() : UUID.fromString("00000000-0000-0000-0000-000000000000"),
                                sd.getDiaryDate(),
                                toSafeTitle(sd.getWorkSummary()),
                                sd.getWorkSummary(),
                                toSafeWeather(sd.getWeather()),
                                null
                        );
                        bridged.setId(sd.getId());
                        bridged.setNew(true);
                        return diaryEntryRepository.save(bridged);
                    });
                } catch (Exception e) {
                    // Nếu luồng khác (ví dụ: upload ảnh) đã tạo bridge thành công trong transaction độc lập,
                    // cập nhật bản ghi đã commit để bảo đảm nhất quán dữ liệu mà không làm hỏng transaction chính.
                    diaryEntryRepository.findById(sd.getId()).ifPresent(entry -> {
                        updateEntryFields(entry, sd);
                        diaryEntryRepository.save(entry);
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * T-72, T-73 (S-31): Lấy thông tin thống kê trạng thái đồng bộ nhật ký của dự án.
     */
    @Transactional(readOnly = true)
    public SiteDiarySyncStatusResponse getProjectSyncStatus(UUID projectId) {
        long total = siteDiaryRepository.countByProjectId(projectId);
        long synced = siteDiaryRepository.countByProjectIdAndSyncStatus(projectId, "SYNCED");
        long pending = siteDiaryRepository.countByProjectIdAndSyncStatus(projectId, "PENDING");
        Optional<SiteDiary> latest = siteDiaryRepository.findFirstByProjectIdOrderByUpdatedAtDesc(projectId);
        OffsetDateTime lastSync = latest.map(SiteDiary::getUpdatedAt).orElse(null);
        return new SiteDiarySyncStatusResponse(projectId, total, synced, pending, lastSync);
    }
}
