package com.ntdhtcct.domain.cost;

import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.cost.dto.ActualCostResponse;
import com.ntdhtcct.domain.cost.dto.ActualCostSummaryResponse;
import com.ntdhtcct.domain.cost.dto.CreateActualCostRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/actual-costs")
public class ActualCostController {

    private final ActualCostService actualCostService;
    private final AuthTokenService authTokenService;

    public ActualCostController(
            ActualCostService actualCostService,
            AuthTokenService authTokenService
    ) {
        this.actualCostService = actualCostService;
        this.authTokenService = authTokenService;
    }

    /**
     * T-94: Xây dựng chức năng nhập chi phí thực tế
     * T-95: Liên kết chi phí với hạng mục tương ứng
     */
    @PostMapping
    public ResponseEntity<?> createActualCost(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateActualCostRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        String createdBy = extractUserId(authorization);

        try {
            ActualCost cost = actualCostService.createActualCost(projectId, request, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(ActualCostResponse.from(cost));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * T-96: Lưu và hiển thị lịch sử chi phí (Lọc theo hạng mục, loại chi phí, ngày phát sinh)
     */
    @GetMapping
    public ResponseEntity<?> getActualCosts(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID wbsItemId,
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<ActualCostResponse> results = actualCostService.getActualCosts(projectId, wbsItemId, costType, startDate, endDate)
                    .stream()
                    .map(ActualCostResponse::from)
                    .toList();
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * T-95 & T-96: Thống kê chi phí thực tế theo dự án hoặc theo hạng mục
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID wbsItemId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            ActualCostSummaryResponse summary = actualCostService.getSummary(projectId, wbsItemId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * T-95 & T-96: Lấy danh sách lịch sử chi phí của riêng một hạng mục
     */
    @GetMapping("/wbs-items/{wbsItemId}")
    public ResponseEntity<?> getActualCostsByWbsItem(
            @PathVariable UUID projectId,
            @PathVariable UUID wbsItemId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<ActualCostResponse> results = actualCostService.getActualCosts(projectId, wbsItemId, null, null, null)
                    .stream()
                    .map(ActualCostResponse::from)
                    .toList();
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        return authTokenService.isTokenValid(authorization.substring(7));
    }

    private String extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            UUID userId = authTokenService.getUserIdFromToken(authorization.substring(7));
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Token không hợp lệ hoặc đã hết hạn"));
    }

    public record ApiResponse(boolean success, String message) {
    }
}
