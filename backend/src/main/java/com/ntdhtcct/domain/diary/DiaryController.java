package com.ntdhtcct.domain.diary;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/diary-entries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<DiaryEntryResponse>> createDiaryEntry(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody CreateDiaryEntryRequest request
    ) {
        UUID authorId = UserSecurityContext.getUserId();
        DiaryEntryResponse response = diaryService.createDiaryEntry(projectId, request, authorId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo nhật ký công trường thành công", response));
    }

    @PostMapping("/{entryId}/images/display")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<DiaryImageResponse>> uploadDisplayImage(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId,
            @RequestParam("clientImageId") String clientImageId,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "capturedAt", required = false) String capturedAtStr,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "requireGps", required = false, defaultValue = "false") boolean requireGps
    ) throws IOException {
        java.time.OffsetDateTime capturedAt = parseOffsetDateTime(capturedAtStr);
        DiaryImageResponse response =
                diaryService.uploadDisplayImage(projectId, entryId, clientImageId, filename, file,
                        capturedAt, latitude, longitude, requireGps);
        return ResponseEntity.ok(ApiResponse.ok("Tải ảnh hiển thị thành công", response));
    }

    @PostMapping("/{entryId}/images/original")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<DiaryImageResponse>> uploadOriginalImage(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId,
            @RequestParam("clientImageId") String clientImageId,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "capturedAt", required = false) String capturedAtStr,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "requireGps", required = false, defaultValue = "false") boolean requireGps
    ) throws IOException {
        java.time.OffsetDateTime capturedAt = parseOffsetDateTime(capturedAtStr);
        DiaryImageResponse response =
                diaryService.uploadOriginalImage(projectId, entryId, clientImageId, filename, file,
                        capturedAt, latitude, longitude, requireGps);
        return ResponseEntity.ok(ApiResponse.ok("Tải ảnh gốc thành công", response));
    }

    @GetMapping("/{entryId}/images/{imageId}/display")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<Resource> getDisplayImage(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId,
            @PathVariable("imageId") UUID imageId,
            @RequestParam(value = "thumb", defaultValue = "false") boolean isThumb
    ) {
        Resource resource = diaryService.getDisplayImageResource(projectId, entryId, imageId, isThumb);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/{entryId}/images/{imageId}/original")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<Resource> getOriginalImage(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId,
            @PathVariable("imageId") UUID imageId
    ) {
        Resource resource = diaryService.getOriginalImageResource(projectId, entryId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"original_" + imageId + "\"")
                .body(resource);
    }

    @GetMapping("/{entryId}/images")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<ApiResponse<java.util.List<DiaryImageResponse>>> getDiaryImages(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId
    ) {
        java.util.List<DiaryImageResponse> images = diaryService.getDiaryImages(projectId, entryId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách ảnh thành công", images));
    }

    @DeleteMapping("/{entryId}/images/{imageId}")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<Void>> deleteDiaryImage(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entryId") UUID entryId,
            @PathVariable("imageId") UUID imageId
    ) {
        diaryService.deleteDiaryImage(projectId, entryId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa ảnh thành công", null));
    }

    private java.time.OffsetDateTime parseOffsetDateTime(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(str);
        } catch (Exception e) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(str);
                return ldt.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
