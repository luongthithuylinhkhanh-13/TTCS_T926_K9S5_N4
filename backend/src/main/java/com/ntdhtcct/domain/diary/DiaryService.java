package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.DiaryDateLockedException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.sitediary.SiteDiary;
import com.ntdhtcct.domain.sitediary.SiteDiaryAdvisoryLockManager;
import com.ntdhtcct.domain.sitediary.SiteDiaryLockService;
import com.ntdhtcct.domain.sitediary.SiteDiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiaryService {

    private static final long MAX_DISPLAY_FILE_SIZE = 1024 * 1024; // < 1 MiB
    private static final long MAX_ORIGINAL_FILE_SIZE = 20 * 1024 * 1024; // <= 20 MiB
    private static final int STRIPE_COUNT = 256;

    private final DiaryEntryRepository diaryEntryRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final SiteDiaryRepository siteDiaryRepository;
    private final SiteDiaryLockService siteDiaryLockService;
    private final SiteDiaryAdvisoryLockManager advisoryLockManager;
    private final ImageMetadataValidator imageMetadataValidator;
    private final Object[] bridgeStripes = new Object[STRIPE_COUNT];
    private final Object[] imageStripes = new Object[STRIPE_COUNT];
    private final TransactionOperations requiresNewTransactionOperations;

    @Autowired
    public DiaryService(DiaryEntryRepository diaryEntryRepository,
                        DiaryImageRepository diaryImageRepository,
                        ProjectRepository projectRepository,
                        FileStorageService fileStorageService,
                        SiteDiaryRepository siteDiaryRepository,
                        SiteDiaryLockService siteDiaryLockService,
                        SiteDiaryAdvisoryLockManager advisoryLockManager,
                        @Autowired(required = false) PlatformTransactionManager transactionManager,
                        @Autowired(required = false) ImageMetadataValidator imageMetadataValidator) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.diaryImageRepository = diaryImageRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
        this.siteDiaryRepository = siteDiaryRepository;
        this.siteDiaryLockService = siteDiaryLockService;
        this.advisoryLockManager = advisoryLockManager;
        this.imageMetadataValidator = imageMetadataValidator != null ? imageMetadataValidator : new ImageMetadataValidator();

        for (int i = 0; i < STRIPE_COUNT; i++) {
            this.bridgeStripes[i] = new Object();
            this.imageStripes[i] = new Object();
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

    public DiaryService(DiaryEntryRepository diaryEntryRepository,
                        DiaryImageRepository diaryImageRepository,
                        ProjectRepository projectRepository,
                        FileStorageService fileStorageService,
                        SiteDiaryRepository siteDiaryRepository,
                        SiteDiaryLockService siteDiaryLockService,
                        SiteDiaryAdvisoryLockManager advisoryLockManager,
                        PlatformTransactionManager transactionManager) {
        this(diaryEntryRepository, diaryImageRepository, projectRepository, fileStorageService,
                siteDiaryRepository, siteDiaryLockService, advisoryLockManager, transactionManager, null);
    }

    public DiaryService(DiaryEntryRepository diaryEntryRepository,
                        DiaryImageRepository diaryImageRepository,
                        ProjectRepository projectRepository,
                        FileStorageService fileStorageService,
                        SiteDiaryRepository siteDiaryRepository,
                        SiteDiaryLockService siteDiaryLockService,
                        SiteDiaryAdvisoryLockManager advisoryLockManager) {
        this(diaryEntryRepository, diaryImageRepository, projectRepository, fileStorageService,
                siteDiaryRepository, siteDiaryLockService, advisoryLockManager, null, null);
    }

    @Transactional
    public DiaryEntryResponse createDiaryEntry(UUID projectId, CreateDiaryEntryRequest request, UUID authorId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Dự án không tồn tại");
        }

        advisoryLockManager.acquireSharedLock(projectId, request.entryDate());
        try {
            if (siteDiaryLockService.isDateLocked(projectId, request.entryDate())) {
                throw new DiaryDateLockedException(
                        "Nhật ký ngày " + request.entryDate() + " đã bị khóa sổ. Thao tác tạo nhật ký bị từ chối."
                );
            }

            DiaryEntry diaryEntry = new DiaryEntry(
                    projectId,
                    authorId,
                    request.entryDate(),
                    request.title(),
                    request.content(),
                    request.weather(),
                    request.wbsItemId()
            );

            DiaryEntry saved = diaryEntryRepository.save(diaryEntry);

            return new DiaryEntryResponse(
                    saved.getId(),
                    saved.getProjectId(),
                    saved.getWbsItemId(),
                    saved.getAuthorId(),
                    saved.getEntryDate(),
                    saved.getTitle(),
                    saved.getContent(),
                    saved.getWeather(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );
        } finally {
            advisoryLockManager.releaseSharedLock(projectId, request.entryDate());
        }
    }

    /**
     * Upload ảnh hiển thị (đã nén phía frontend, kích thước < 1 MiB).
     */
    @Transactional
    public DiaryImageResponse uploadDisplayImage(UUID projectId, UUID entryId, String clientImageId,
                                                String filename, MultipartFile file) throws IOException {
        return uploadDisplayImage(projectId, entryId, clientImageId, filename, file, null, null, null, false);
    }

    /**
     * Upload ảnh hiển thị kèm siêu dữ liệu (T-74 / S-42).
     */
    @Transactional
    public DiaryImageResponse uploadDisplayImage(UUID projectId, UUID entryId, String clientImageId,
                                                String filename, MultipartFile file,
                                                OffsetDateTime capturedAt, Double latitude, Double longitude,
                                                boolean requireGps) throws IOException {
        LocalDate diaryDate = resolveDiaryDate(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, diaryDate);
        try {
            if (siteDiaryLockService.isDateLocked(projectId, diaryDate)) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + diaryDate + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                );
            }

            DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);
            if (!entry.getEntryDate().equals(diaryDate)) {
                if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                    throw new DiaryDateLockedException(
                            "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                    );
                }
            }

            if (file.getSize() >= MAX_DISPLAY_FILE_SIZE) {
                throw new BadRequestException("FILE_TOO_LARGE", "Ảnh hiển thị phải có dung lượng nhỏ hơn 1 MiB");
            }

            // T-74: Trích xuất và kiểm tra siêu dữ liệu (metadata) của ảnh hiện trường
            ExifMetadataExtractor.ParsedExif parsedExif = ExifMetadataExtractor.extract(file.getBytes());
            OffsetDateTime effectiveCapturedAt = capturedAt != null ? capturedAt : parsedExif.capturedAt();
            Double effectiveLat = latitude != null ? latitude : parsedExif.latitude();
            Double effectiveLon = longitude != null ? longitude : parsedExif.longitude();

            ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                    filename,
                    file.getContentType(),
                    file.getSize(),
                    effectiveCapturedAt,
                    effectiveLat,
                    effectiveLon,
                    null,
                    null
            );
            imageMetadataValidator.validate(meta, requireGps, false);

            UUID attemptId = UUID.randomUUID();

            // Lấy hoặc khởi tạo bản ghi DiaryImage với siêu dữ liệu
            DiaryImage image = getOrCreateImageRecord(entry.getId(), clientImageId, filename, file.getContentType(),
                    effectiveCapturedAt, effectiveLat, effectiveLon);
            diaryImageRepository.startDisplayUpload(image.getId(), attemptId, java.time.OffsetDateTime.now());

            // Lưu file vật lý và sinh thumbnail ngoài filesystem
            FileStorageService.StoredDisplayResult stored =
                    fileStorageService.storeDisplayFile(entry.getId(), clientImageId, attemptId, file);

            // Cập nhật an toàn với attemptId để chống race condition retry
            int updatedRows = diaryImageRepository.updateDisplayCompletedIfAttemptMatches(
                    image.getId(),
                    attemptId,
                    stored.displayPath(),
                    stored.fileSize(),
                    stored.thumbnailPath(),
                    stored.width(),
                    stored.height(),
                    java.time.OffsetDateTime.now()
            );

            if (updatedRows == 0) {
                // Attempt đã bị một attempt mới hơn ghi đè
                image = diaryImageRepository.findById(image.getId()).orElseThrow();
            } else {
                image = diaryImageRepository.findById(image.getId()).orElseThrow();
            }

            return toImageResponse(image);
        } finally {
            advisoryLockManager.releaseSharedLock(projectId, diaryDate);
        }
    }

    /**
     * Upload ảnh gốc (giữ nguyên bytes, dung lượng tối đa 20 MiB).
     */
    @Transactional
    public DiaryImageResponse uploadOriginalImage(UUID projectId, UUID entryId, String clientImageId,
                                                 String filename, MultipartFile file) throws IOException {
        return uploadOriginalImage(projectId, entryId, clientImageId, filename, file, null, null, null, false);
    }

    /**
     * Upload ảnh gốc kèm siêu dữ liệu (T-74 / S-42).
     */
    @Transactional
    public DiaryImageResponse uploadOriginalImage(UUID projectId, UUID entryId, String clientImageId,
                                                 String filename, MultipartFile file,
                                                 OffsetDateTime capturedAt, Double latitude, Double longitude,
                                                 boolean requireGps) throws IOException {
        LocalDate diaryDate = resolveDiaryDate(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, diaryDate);
        try {
            if (siteDiaryLockService.isDateLocked(projectId, diaryDate)) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + diaryDate + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                );
            }

            DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);
            if (!entry.getEntryDate().equals(diaryDate)) {
                if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                    throw new DiaryDateLockedException(
                            "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                    );
                }
            }

            if (file.getSize() > MAX_ORIGINAL_FILE_SIZE) {
                throw new BadRequestException("FILE_TOO_LARGE", "Ảnh gốc vượt quá giới hạn tối đa 20 MiB");
            }

            // T-74: Trích xuất và kiểm tra siêu dữ liệu (metadata) của ảnh hiện trường
            ExifMetadataExtractor.ParsedExif parsedExif = ExifMetadataExtractor.extract(file.getBytes());
            OffsetDateTime effectiveCapturedAt = capturedAt != null ? capturedAt : parsedExif.capturedAt();
            Double effectiveLat = latitude != null ? latitude : parsedExif.latitude();
            Double effectiveLon = longitude != null ? longitude : parsedExif.longitude();

            ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                    filename,
                    file.getContentType(),
                    file.getSize(),
                    effectiveCapturedAt,
                    effectiveLat,
                    effectiveLon,
                    null,
                    null
            );
            imageMetadataValidator.validate(meta, requireGps, false);

            UUID attemptId = UUID.randomUUID();

            // Lấy hoặc khởi tạo bản ghi DiaryImage với siêu dữ liệu
            DiaryImage image = getOrCreateImageRecord(entry.getId(), clientImageId, filename, file.getContentType(),
                    effectiveCapturedAt, effectiveLat, effectiveLon);
            diaryImageRepository.startOriginalUpload(image.getId(), attemptId, java.time.OffsetDateTime.now());

            // Lưu file nguyên trạng ngoài filesystem
            FileStorageService.StoredOriginalResult stored =
                    fileStorageService.storeOriginalFile(entry.getId(), clientImageId, attemptId, file);

            int updatedRows = diaryImageRepository.updateOriginalCompletedIfAttemptMatches(
                    image.getId(),
                    attemptId,
                    stored.originalPath(),
                    stored.fileSize(),
                    java.time.OffsetDateTime.now()
            );

            if (updatedRows == 0) {
                image = diaryImageRepository.findById(image.getId()).orElseThrow();
            } else {
                image = diaryImageRepository.findById(image.getId()).orElseThrow();
            }

            return toImageResponse(image);
        } finally {
            advisoryLockManager.releaseSharedLock(projectId, diaryDate);
        }
    }

    /**
     * Xóa ảnh đính kèm khỏi nhật ký.
     * Chặn nếu ngày nhật ký đã bị khóa sổ.
     */
    @Transactional
    public void deleteDiaryImage(UUID projectId, UUID entryId, UUID imageId) {
        LocalDate diaryDate = resolveDiaryDate(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, diaryDate);
        try {
            if (siteDiaryLockService.isDateLocked(projectId, diaryDate)) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + diaryDate + " đã bị khóa sổ. Thao tác xóa ảnh bị từ chối."
                );
            }

            DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);
            if (!entry.getEntryDate().equals(diaryDate)) {
                if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                    throw new DiaryDateLockedException(
                            "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác xóa ảnh bị từ chối."
                    );
                }
            }

            DiaryImage image = diaryImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

            if (!image.getDiaryEntryId().equals(entry.getId())) {
                throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Ảnh không thuộc mục nhật ký này");
            }

            // Xóa file vật lý
            fileStorageService.deleteFile(image.getThumbnailPath());
            fileStorageService.deleteFile(image.getDisplayPath());
            fileStorageService.deleteFile(image.getOriginalPath());

            diaryImageRepository.delete(image);
        } finally {
            advisoryLockManager.releaseSharedLock(projectId, diaryDate);
        }
    }

    /**
     * Tải file hiển thị hoặc thumbnail chỉ khi trạng thái displayStatus = COMPLETED.
     */
    @Transactional(readOnly = true)
    public Resource getDisplayImageResource(UUID projectId, UUID entryId, UUID imageId, boolean isThumbnail) {
        DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);

        DiaryImage image = diaryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

        if (!image.getDiaryEntryId().equals(entry.getId())) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Ảnh không thuộc mục nhật ký này");
        }

        boolean isDisplayCompleted = "COMPLETED".equalsIgnoreCase(image.getDisplayStatus());
        boolean isOriginalCompleted = "COMPLETED".equalsIgnoreCase(image.getOriginalStatus());

        if (!isDisplayCompleted && !isOriginalCompleted) {
            throw new BadRequestException("IMAGE_NOT_READY", "Ảnh hiển thị chưa hoàn tất quá trình tải lên");
        }

        Path filePath = null;
        if (isThumbnail && image.getThumbnailPath() != null && !image.getThumbnailPath().isBlank()) {
            try {
                filePath = fileStorageService.loadFile(image.getThumbnailPath());
            } catch (ResourceNotFoundException ignored) {
                // fallback to displayPath or originalPath
            }
        }

        if (filePath == null) {
            String pathStr = image.getDisplayPath() != null && !image.getDisplayPath().isBlank()
                    ? image.getDisplayPath()
                    : image.getOriginalPath();
            filePath = fileStorageService.loadFile(pathStr);
        }

        return new FileSystemResource(filePath);
    }

    /**
     * Tải file gốc chỉ khi originalStatus = COMPLETED.
     */
    @Transactional(readOnly = true)
    public Resource getOriginalImageResource(UUID projectId, UUID entryId, UUID imageId) {
        DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);

        DiaryImage image = diaryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

        if (!image.getDiaryEntryId().equals(entry.getId())) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Ảnh không thuộc mục nhật ký này");
        }

        if (!"COMPLETED".equalsIgnoreCase(image.getOriginalStatus())) {
            throw new BadRequestException("IMAGE_NOT_READY", "Ảnh gốc chưa hoàn tất quá trình tải lên");
        }

        Path filePath = fileStorageService.loadFile(image.getOriginalPath());
        return new FileSystemResource(filePath);
    }

    /**
     * T-63: Lấy danh sách ảnh của mục nhật ký (sắp xếp theo sort_order và created_at).
     */
    @Transactional(readOnly = true)
    public List<DiaryImageResponse> getDiaryImages(UUID projectId, UUID entryId) {
        DiaryEntry entry = resolveOrCreateEntry(projectId, entryId);
        return diaryImageRepository.findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(entry.getId())
                .stream()
                .map(this::toImageResponse)
                .toList();
    }

    private Object getBridgeLock(UUID entryId) {
        if (entryId == null) {
            return bridgeStripes[0];
        }
        int index = (entryId.hashCode() & 0x7fffffff) % STRIPE_COUNT;
        return bridgeStripes[index];
    }

    private Object getImageLock(UUID entryId, String clientImageId) {
        String key = (entryId != null ? entryId.toString() : "") + ":" + (clientImageId != null ? clientImageId : "");
        int index = (key.hashCode() & 0x7fffffff) % STRIPE_COUNT;
        return imageStripes[index];
    }

    /**
     * Xác định ngày nhật ký và kiểm tra thuộc về dự án trước khi thực hiện thao tác.
     */
    private LocalDate resolveDiaryDate(UUID projectId, UUID entryId) {
        Optional<DiaryEntry> entryOpt = diaryEntryRepository.findById(entryId);
        if (entryOpt.isPresent()) {
            DiaryEntry entry = entryOpt.get();
            if (!entry.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            return entry.getEntryDate();
        }

        Optional<SiteDiary> siteDiaryOpt = siteDiaryRepository.findById(entryId);
        if (siteDiaryOpt.isPresent()) {
            SiteDiary sd = siteDiaryOpt.get();
            if (!sd.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            return sd.getDiaryDate();
        }

        throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Không tìm thấy mục nhật ký");
    }

    /**
     * Kiểm tra và giải quyết mục nhật ký: hỗ trợ cả DiaryEntry lẫn SiteDiary (bridge).
     */
    private DiaryEntry resolveOrCreateEntry(UUID projectId, UUID entryId) {
        Optional<DiaryEntry> entryOpt = diaryEntryRepository.findById(entryId);
        if (entryOpt.isPresent()) {
            DiaryEntry entry = entryOpt.get();
            if (!entry.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            return entry;
        }

        Optional<SiteDiary> siteDiaryOpt = siteDiaryRepository.findById(entryId);
        if (siteDiaryOpt.isPresent()) {
            SiteDiary sd = siteDiaryOpt.get();
            if (!sd.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            return getOrCreateBridgedEntry(sd);
        }

        throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Không tìm thấy mục nhật ký");
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

    private DiaryEntry getOrCreateBridgedEntry(SiteDiary sd) {
        Optional<DiaryEntry> existing = diaryEntryRepository.findById(sd.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Object lock = getBridgeLock(sd.getId());
        synchronized (lock) {
            existing = diaryEntryRepository.findById(sd.getId());
            if (existing.isPresent()) {
                return existing.get();
            }

            try {
                return requiresNewTransactionOperations.execute(status -> {
                    Optional<DiaryEntry> inTx = diaryEntryRepository.findById(sd.getId());
                    if (inTx.isPresent()) {
                        return inTx.get();
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
                return diaryEntryRepository.findById(sd.getId())
                        .orElseThrow(() -> new IllegalStateException("Không thể tạo hoặc tải DiaryEntry bridge cho ID: " + sd.getId(), e));
            }
        }
    }

    private DiaryImage getOrCreateImageRecord(UUID entryId, String clientImageId, String filename, String contentType) {
        return getOrCreateImageRecord(entryId, clientImageId, filename, contentType, null, null, null);
    }

    private DiaryImage getOrCreateImageRecord(UUID entryId, String clientImageId, String filename, String contentType,
                                              OffsetDateTime capturedAt, Double latitude, Double longitude) {
        Optional<DiaryImage> existing = diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, clientImageId);
        if (existing.isPresent()) {
            DiaryImage img = existing.get();
            boolean changed = false;
            if (img.getCapturedAt() == null && capturedAt != null) {
                img.setCapturedAt(capturedAt);
                changed = true;
            }
            if (img.getLatitude() == null && latitude != null) {
                img.setLatitude(latitude);
                changed = true;
            }
            if (img.getLongitude() == null && longitude != null) {
                img.setLongitude(longitude);
                changed = true;
            }
            if (changed) {
                return diaryImageRepository.save(img);
            }
            return img;
        }

        Object lock = getImageLock(entryId, clientImageId);
        synchronized (lock) {
            existing = diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, clientImageId);
            if (existing.isPresent()) {
                DiaryImage img = existing.get();
                boolean changed = false;
                if (img.getCapturedAt() == null && capturedAt != null) {
                    img.setCapturedAt(capturedAt);
                    changed = true;
                }
                if (img.getLatitude() == null && latitude != null) {
                    img.setLatitude(latitude);
                    changed = true;
                }
                if (img.getLongitude() == null && longitude != null) {
                    img.setLongitude(longitude);
                    changed = true;
                }
                if (changed) {
                    return diaryImageRepository.save(img);
                }
                return img;
            }

            try {
                return requiresNewTransactionOperations.execute(status -> {
                    Optional<DiaryImage> inTx = diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, clientImageId);
                    if (inTx.isPresent()) {
                        return inTx.get();
                    }
                    DiaryImage newImage = new DiaryImage(
                            entryId,
                            clientImageId,
                            filename != null ? filename : "image.jpg",
                            contentType != null ? contentType : "image/jpeg"
                    );
                    newImage.setCapturedAt(capturedAt);
                    newImage.setLatitude(latitude);
                    newImage.setLongitude(longitude);
                    return diaryImageRepository.save(newImage);
                });
            } catch (Exception e) {
                return diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, clientImageId)
                        .orElseThrow(() -> new IllegalStateException("Không thể tạo hoặc tải DiaryImage cho clientImageId: " + clientImageId, e));
            }
        }
    }

    private DiaryImageResponse toImageResponse(DiaryImage img) {
        return new DiaryImageResponse(
                img.getId(),
                img.getDiaryEntryId(),
                img.getClientImageId(),
                img.getOriginalFilename(),
                img.getDisplayStatus(),
                img.getOriginalStatus(),
                img.getDisplayFileSize(),
                img.getOriginalFileSize(),
                img.getWidth(),
                img.getHeight(),
                img.getCapturedAt(),
                img.getLatitude(),
                img.getLongitude(),
                img.getCreatedAt(),
                img.getUpdatedAt()
        );
    }
}
