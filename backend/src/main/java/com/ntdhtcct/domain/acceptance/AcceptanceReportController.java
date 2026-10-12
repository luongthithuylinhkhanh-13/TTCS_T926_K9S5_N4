package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.domain.acceptance.dto.AcceptanceReportResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceSummaryResponse;
import com.ntdhtcct.domain.acceptance.dto.CreateAcceptanceReportRequest;
import com.ntdhtcct.domain.acceptance.dto.UpdateAcceptanceReportRequest;
import com.ntdhtcct.domain.auth.AuthTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller tiếp nhận yêu cầu liên quan đến Lập phiếu nghiệm thu theo đợt.
 * Phục vụ Story S-36 (NTDHTCT-220):
 *   - NTDHTCT-268 (T-83): Tạo phiếu nghiệm thu theo từng đợt
 *   - NTDHTCT-269 (T-84): Chọn hạng mục và nhập khối lượng nghiệm thu
 *   - NTDHTCT-270 (T-85): Tự động tính giá trị nghiệm thu của phiếu
 */
@RestController
@RequestMapping("/api/projects/{projectId}/acceptance-reports")
public class AcceptanceReportController {

    private final AcceptanceReportService acceptanceReportService;
    private final AuthTokenService authTokenService;

    public AcceptanceReportController(
            AcceptanceReportService acceptanceReportService,
            AuthTokenService authTokenService
    ) {
        this.acceptanceReportService = acceptanceReportService;
        this.authTokenService = authTokenService;
    }

    /**
     * T-83: Lấy danh sách phiếu nghiệm thu theo từng đợt của dự án.
     */
    @GetMapping
    public ResponseEntity<List<AcceptanceReportResponse>> getReports(@PathVariable UUID projectId) {
        List<AcceptanceReportResponse> list = acceptanceReportService.getAcceptanceReportsByProject(projectId);
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy tóm tắt thống kê nghiệm thu của dự án.
     */
    @GetMapping("/summary")
    public ResponseEntity<AcceptanceSummaryResponse> getSummary(@PathVariable UUID projectId) {
        AcceptanceSummaryResponse summary = acceptanceReportService.getProjectAcceptanceSummary(projectId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Lấy số đợt nghiệm thu gợi ý tiếp theo cho dự án.
     */
    @GetMapping("/next-period")
    public ResponseEntity<?> getNextPeriod(@PathVariable UUID projectId) {
        int nextPeriod = acceptanceReportService.getNextPeriodNumber(projectId);
        return ResponseEntity.ok(Map.of(
                "nextPeriodNumber", nextPeriod,
                "suggestedReportNumber", String.format("NT-%02d", nextPeriod)
        ));
    }

    /**
     * Xem chi tiết một phiếu nghiệm thu kèm các hạng mục và giá trị tính toán.
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReportById(@PathVariable UUID projectId, @PathVariable UUID reportId) {
        return acceptanceReportService.getAcceptanceReportById(projectId, reportId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * T-83, T-84, T-85: Tạo mới phiếu nghiệm thu theo đợt.
     */
    @PostMapping
    public ResponseEntity<?> createReport(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateAcceptanceReportRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        try {
            UUID creatorId = resolveUserId(authorization, xUserId);
            AcceptanceReportResponse response = acceptanceReportService.createAcceptanceReport(projectId, creatorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Cập nhật phiếu nghiệm thu.
     */
    @PutMapping("/{reportId}")
    public ResponseEntity<?> updateReport(
            @PathVariable UUID projectId,
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateAcceptanceReportRequest request
    ) {
        try {
            return acceptanceReportService.updateAcceptanceReport(projectId, reportId, request)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Xóa phiếu nghiệm thu (khi chưa được phê duyệt).
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(
            @PathVariable UUID projectId,
            @PathVariable UUID reportId
    ) {
        try {
            boolean deleted = acceptanceReportService.deleteAcceptanceReport(projectId, reportId);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
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
