package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
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

    public DiaryService(DiaryEntryRepository diaryEntryRepository,
                        DiaryImageRepository diaryImageRepository,
                        ProjectRepository projectRepository,
                        FileStorageService fileStorageService) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.diaryImageRepository = diaryImageRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public DiaryEntryResponse createDiaryEntry(UUID projectId, CreateDiaryEntryRequest request, UUID authorId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Dự án không tồn tại");
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
    }

    /**
     * Upload ảnh hiển thị (đã nén phía frontend, kích thước < 1 MiB).
     */
    @Transactional
    public DiaryImageResponse uploadDisplayImage(UUID projectId, UUID entryId, String clientImageId,
                                                String filename, MultipartFile file) throws IOException {
        validateEntryBelongsToProject(projectId, entryId);

        if (file.getSize() >= MAX_DISPLAY_FILE_SIZE) {
            throw new BadRequestException("FILE_TOO_LARGE", "Ảnh hiển thị phải có dung lượng nhỏ hơn 1 MiB");
        }

        UUID attemptId = UUID.randomUUID();

        // Lấy hoặc khởi tạo bản ghi DiaryImage
        DiaryImage image = getOrCreateImageRecord(entryId, clientImageId, filename, file.getContentType());
        image.setDisplayAttemptId(attemptId);
        image.setDisplayStatus("UPLOADING");
        diaryImageRepository.saveAndFlush(image);

        // Lưu file vật lý và sinh thumbnail ngoài filesystem
        FileStorageService.StoredDisplayResult stored =
                fileStorageService.storeDisplayFile(entryId, clientImageId, attemptId, file);

        // Cập nhật an toàn với attemptId để chống race condition retry
        int updatedRows = diaryImageRepository.updateDisplayCompletedIfAttemptMatches(
                image.getId(),
                attemptId,
                stored.displayPath(),
                stored.fileSize(),
                stored.thumbnailPath(),
                stored.width(),
                stored.height()
        );

        if (updatedRows == 0) {
            // Attempt đã bị một attempt mới hơn ghi đè
            image = diaryImageRepository.findById(image.getId()).orElseThrow();
        } else {
            image = diaryImageRepository.findById(image.getId()).orElseThrow();
        }

        return toImageResponse(image);
    }

    /**
     * Upload ảnh gốc (giữ nguyên bytes, dung lượng tối đa 20 MiB).
     */
    @Transactional
    public DiaryImageResponse uploadOriginalImage(UUID projectId, UUID entryId, String clientImageId,
                                                 String filename, MultipartFile file) throws IOException {
        validateEntryBelongsToProject(projectId, entryId);

        if (file.getSize() > MAX_ORIGINAL_FILE_SIZE) {
            throw new BadRequestException("FILE_TOO_LARGE", "Ảnh gốc vượt quá giới hạn tối đa 20 MiB");
        }

        UUID attemptId = UUID.randomUUID();

        // Lấy hoặc khởi tạo bản ghi DiaryImage
        DiaryImage image = getOrCreateImageRecord(entryId, clientImageId, filename, file.getContentType());
        image.setOriginalAttemptId(attemptId);
        image.setOriginalStatus("UPLOADING");
        diaryImageRepository.saveAndFlush(image);

        // Lưu file nguyên trạng ngoài filesystem
        FileStorageService.StoredOriginalResult stored =
                fileStorageService.storeOriginalFile(entryId, clientImageId, attemptId, file);

        int updatedRows = diaryImageRepository.updateOriginalCompletedIfAttemptMatches(
                image.getId(),
                attemptId,
                stored.originalPath(),
                stored.fileSize()
        );

        if (updatedRows == 0) {
            image = diaryImageRepository.findById(image.getId()).orElseThrow();
        } else {
            image = diaryImageRepository.findById(image.getId()).orElseThrow();
        }

        return toImageResponse(image);
    }

    /**
     * Tải file hiển thị hoặc thumbnail chỉ khi trạng thái displayStatus = COMPLETED.
     */
    @Transactional(readOnly = true)
    public Resource getDisplayImageResource(UUID projectId, UUID entryId, UUID imageId, boolean isThumbnail) {
        validateEntryBelongsToProject(projectId, entryId);

        DiaryImage image = diaryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

        if (!image.getDiaryEntryId().equals(entryId)) {
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
        validateEntryBelongsToProject(projectId, entryId);

        DiaryImage image = diaryImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "Không tìm thấy ảnh"));

        if (!image.getDiaryEntryId().equals(entryId)) {
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
        validateEntryBelongsToProject(projectId, entryId);
        return diaryImageRepository.findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(entryId)
                .stream()
                .map(this::toImageResponse)
                .toList();
    }

    private void validateEntryBelongsToProject(UUID projectId, UUID entryId) {
        DiaryEntry entry = diaryEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ENTRY_NOT_FOUND", "Không tìm thấy mục nhật ký"));
        if (!entry.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("ENTRY_NOT_FOUND", "Mục nhật ký không thuộc dự án chỉ định");
        }
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
