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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiaryService {

    private static final long MAX_DISPLAY_FILE_SIZE = 1024 * 1024; // < 1 MiB
    private static final long MAX_ORIGINAL_FILE_SIZE = 20 * 1024 * 1024; // <= 20 MiB

    private final DiaryEntryRepository diaryEntryRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final SiteDiaryRepository siteDiaryRepository;
    private final SiteDiaryLockService siteDiaryLockService;
    private final SiteDiaryAdvisoryLockManager advisoryLockManager;

    public DiaryService(DiaryEntryRepository diaryEntryRepository,
                        DiaryImageRepository diaryImageRepository,
                        ProjectRepository projectRepository,
                        FileStorageService fileStorageService,
                        SiteDiaryRepository siteDiaryRepository,
                        SiteDiaryLockService siteDiaryLockService,
                        SiteDiaryAdvisoryLockManager advisoryLockManager) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.diaryImageRepository = diaryImageRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
        this.siteDiaryRepository = siteDiaryRepository;
        this.siteDiaryLockService = siteDiaryLockService;
        this.advisoryLockManager = advisoryLockManager;
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
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, entry.getEntryDate());
        try {
            if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                );
            }

            if (file.getSize() >= MAX_DISPLAY_FILE_SIZE) {
                throw new BadRequestException("FILE_TOO_LARGE", "Ảnh hiển thị phải có dung lượng nhỏ hơn 1 MiB");
            }

            UUID attemptId = UUID.randomUUID();

            // Lấy hoặc khởi tạo bản ghi DiaryImage
            DiaryImage image = getOrCreateImageRecord(entry.getId(), clientImageId, filename, file.getContentType());
            image.setDisplayAttemptId(attemptId);
            image.setDisplayStatus("UPLOADING");
            diaryImageRepository.saveAndFlush(image);

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
            advisoryLockManager.releaseSharedLock(projectId, entry.getEntryDate());
        }
    }

    /**
     * Upload ảnh gốc (giữ nguyên bytes, dung lượng tối đa 20 MiB).
     */
    @Transactional
    public DiaryImageResponse uploadOriginalImage(UUID projectId, UUID entryId, String clientImageId,
                                                 String filename, MultipartFile file) throws IOException {
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, entry.getEntryDate());
        try {
            if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác tải ảnh bị từ chối."
                );
            }

            if (file.getSize() > MAX_ORIGINAL_FILE_SIZE) {
                throw new BadRequestException("FILE_TOO_LARGE", "Ảnh gốc vượt quá giới hạn tối đa 20 MiB");
            }

            UUID attemptId = UUID.randomUUID();

            // Lấy hoặc khởi tạo bản ghi DiaryImage
            DiaryImage image = getOrCreateImageRecord(entry.getId(), clientImageId, filename, file.getContentType());
            image.setOriginalAttemptId(attemptId);
            image.setOriginalStatus("UPLOADING");
            diaryImageRepository.saveAndFlush(image);

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
            advisoryLockManager.releaseSharedLock(projectId, entry.getEntryDate());
        }
    }

    /**
     * Xóa ảnh đính kèm khỏi nhật ký.
     * Chặn nếu ngày nhật ký đã bị khóa sổ.
     */
    @Transactional
    public void deleteDiaryImage(UUID projectId, UUID entryId, UUID imageId) {
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);

        advisoryLockManager.acquireSharedLock(projectId, entry.getEntryDate());
        try {
            if (siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())) {
                throw new DiaryDateLockedException(
                        "Nhật ký công trường ngày " + entry.getEntryDate() + " đã bị khóa sổ. Thao tác xóa ảnh bị từ chối."
                );
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
            advisoryLockManager.releaseSharedLock(projectId, entry.getEntryDate());
        }
    }

    /**
     * Tải file hiển thị hoặc thumbnail chỉ khi trạng thái displayStatus = COMPLETED.
     */
    @Transactional(readOnly = true)
    public Resource getDisplayImageResource(UUID projectId, UUID entryId, UUID imageId, boolean isThumbnail) {
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);

        DiaryImage image = diaryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

        if (!image.getDiaryEntryId().equals(entry.getId())) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Ảnh không thuộc mục nhật ký này");
        }

        if (!"COMPLETED".equalsIgnoreCase(image.getDisplayStatus())) {
            throw new BadRequestException("IMAGE_NOT_READY", "Ảnh hiển thị chưa hoàn tất quá trình tải lên");
        }

        String pathStr = isThumbnail ? image.getThumbnailPath() : image.getDisplayPath();
        Path filePath = fileStorageService.loadFile(pathStr);
        return new FileSystemResource(filePath);
    }

    /**
     * Tải file gốc chỉ khi originalStatus = COMPLETED.
     */
    @Transactional(readOnly = true)
    public Resource getOriginalImageResource(UUID projectId, UUID entryId, UUID imageId) {
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);

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
        DiaryEntry entry = resolveAndValidateEntry(projectId, entryId);
        return diaryImageRepository.findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(entry.getId())
                .stream()
                .map(this::toImageResponse)
                .toList();
    }

    /**
     * Kiểm tra và giải quyết mục nhật ký: hỗ trợ cả DiaryEntry lẫn SiteDiary (bridge).
     */
    private DiaryEntry resolveAndValidateEntry(UUID projectId, UUID entryId) {
        Optional<DiaryEntry> entryOpt = diaryEntryRepository.findById(entryId);
        if (entryOpt.isPresent()) {
            DiaryEntry entry = entryOpt.get();
            if (!entry.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            return entry;
        }

        // Kiểm tra xem entryId có thuộc site_diaries không (cùng ID)
        Optional<SiteDiary> siteDiaryOpt = siteDiaryRepository.findById(entryId);
        if (siteDiaryOpt.isPresent()) {
            SiteDiary sd = siteDiaryOpt.get();
            if (!sd.getProjectId().equals(projectId)) {
                throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
            }
            DiaryEntry bridged = new DiaryEntry(
                    sd.getProjectId(),
                    sd.getReporterId() != null ? sd.getReporterId() : UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    sd.getDiaryDate(),
                    sd.getWorkSummary() != null && !sd.getWorkSummary().isBlank() ? sd.getWorkSummary() : "Nhật ký thi công",
                    sd.getWorkSummary(),
                    sd.getWeather(),
                    null
            );
            bridged.setId(sd.getId());
            return diaryEntryRepository.save(bridged);
        }

        throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Không tìm thấy mục nhật ký");
    }

    private DiaryImage getOrCreateImageRecord(UUID entryId, String clientImageId, String filename, String contentType) {
        Optional<DiaryImage> existing = diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, clientImageId);
        if (existing.isPresent()) {
            return existing.get();
        }

        DiaryImage newImage = new DiaryImage(
                entryId,
                clientImageId,
                filename != null ? filename : "image.jpg",
                contentType != null ? contentType : "image/jpeg"
        );
        return diaryImageRepository.save(newImage);
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
                img.getCreatedAt(),
                img.getUpdatedAt()
        );
    }
}
