package com.ntdhtcct.domain.sitediary;

import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryBatchSyncResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiarySyncResult;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUpdateRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryLockRequest;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryLockResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUnlockRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller tiếp nhận và xử lý đồng bộ nhật ký công trường.
 * Phục vụ Story S-30 (NTDHTCT-214) và S-24 (T-64: Khóa sổ nhật ký theo ngày).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/site-diaries")
public class SiteDiaryController {

    private final SiteDiarySyncService siteDiarySyncService;
    private final SiteDiaryLockService siteDiaryLockService;
    private final AuthTokenService authTokenService;

    public SiteDiaryController(
            SiteDiarySyncService siteDiarySyncService,
            SiteDiaryLockService siteDiaryLockService,
            AuthTokenService authTokenService) {
        this.siteDiarySyncService = siteDiarySyncService;
        this.siteDiaryLockService = siteDiaryLockService;
        this.authTokenService = authTokenService;
    }

    /**
     * T-71: Đồng bộ một bản ghi nhật ký công trường từ hàng đợi ngoại tuyến.
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncSingleDiary(
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteDiarySyncRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        UUID reporterId = resolveUserId(authorization, xUserId);
        SiteDiarySyncResult result = siteDiarySyncService.syncSingleDiary(projectId, reporterId, request);

        if ("LOCKED".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        if ("ERROR".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
        if ("ALREADY_EXISTS".equals(result.getStatus())) {
            // Trả về HTTP 200 OK thông báo đã tồn tại (idempotent)
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * T-71: Tự động gửi hàng loạt dữ liệu chưa đồng bộ từ hàng đợi lên máy chủ khi có mạng trở lại.
     */
    @PostMapping("/batch-sync")
    public ResponseEntity<SiteDiaryBatchSyncResponse> batchSyncDiaries(
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteDiaryBatchSyncRequest batchRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        UUID reporterId = resolveUserId(authorization, xUserId);
        SiteDiaryBatchSyncResponse response = siteDiarySyncService.syncBatchDiaries(projectId, reporterId, batchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo nhật ký công trường trực tiếp khi trực tuyến.
     */
    @PostMapping
    public ResponseEntity<?> createDiary(
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteDiarySyncRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        return syncSingleDiary(projectId, request, authorization, xUserId);
    }

    /**
     * Danh sách nhật ký công trường của dự án.
     */
    @GetMapping
    public ResponseEntity<List<SiteDiaryResponse>> getDiaries(@PathVariable UUID projectId) {
        List<SiteDiaryResponse> list = siteDiarySyncService.getDiariesByProject(projectId);
        return ResponseEntity.ok(list);
    }

    /**
     * Xem chi tiết một bản ghi nhật ký.
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<?> getDiaryById(@PathVariable UUID projectId, @PathVariable UUID diaryId) {
        return siteDiarySyncService.getDiaryById(projectId, diaryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cập nhật nhật ký đã ghi trong dự án.
     */
    @PutMapping("/{diaryId}")
    public ResponseEntity<?> updateDiary(
            @PathVariable UUID projectId,
            @PathVariable UUID diaryId,
            @Valid @RequestBody SiteDiaryUpdateRequest request
    ) {
        return siteDiarySyncService.updateDiary(projectId, diaryId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * T-64: Khóa sổ nhật ký công trường theo ngày (Yêu cầu ADMIN hoặc PROJECT_MANAGER).
     */
    @PostMapping("/locks")
    public ResponseEntity<SiteDiaryLockResponse> lockDiaryDate(
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteDiaryLockRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        UUID userId = resolveUserId(authorization, xUserId);
        SiteDiaryLockResponse response = siteDiaryLockService.lockDate(
                projectId,
                userId,
                request.lockDate(),
                request.reason()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * T-64: Mở khóa sổ nhật ký công trường theo ngày (Yêu cầu ADMIN hoặc PROJECT_MANAGER).
     */
    @PostMapping("/unlocks")
    public ResponseEntity<SiteDiaryLockResponse> unlockDiaryDate(
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteDiaryUnlockRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        UUID userId = resolveUserId(authorization, xUserId);
        SiteDiaryLockResponse response = siteDiaryLockService.unlockDate(
                projectId,
                userId,
                request.lockDate(),
                request.reason()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * T-64: Danh sách tất cả các ngày đã thiết lập khóa/mở khóa sổ của dự án.
     */
    @GetMapping("/locks")
    public ResponseEntity<List<SiteDiaryLockResponse>> getProjectLocks(@PathVariable UUID projectId) {
        List<SiteDiaryLockResponse> response = siteDiaryLockService.getProjectLocks(projectId);
        return ResponseEntity.ok(response);
    }

    /**
     * T-64: Tra cứu trạng thái khóa sổ của một ngày cụ thể trong dự án.
     */
    @GetMapping("/locks/status")
    public ResponseEntity<SiteDiaryLockResponse> getLockStatus(
            @PathVariable UUID projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return siteDiaryLockService.getLockStatus(projectId, date)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new SiteDiaryLockResponse(
                        null,
                        projectId,
                        date,
                        "UNLOCKED",
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )));
    }

    private UUID resolveUserId(String authorization, String xUserId) {
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                return UUID.fromString(xUserId);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                return authTokenService.getUserIdFromToken(token);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
