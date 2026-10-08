package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.auth.AuthTokenService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/daily-reports")
public class DailyReportController {

    private final DailyReportService dailyReportService;
    private final AuthTokenService authTokenService;

    public DailyReportController(DailyReportService dailyReportService, AuthTokenService authTokenService) {
        this.dailyReportService = dailyReportService;
        this.authTokenService = authTokenService;
    }

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody DailyReportRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UUID userId = getUserId(authorization);
        if (userId == null) {
            return unauthorized();
        }

        try {
            DailyReportResponse response = dailyReportService.createReport(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/wbs-items/{wbsItemId}")
    public ResponseEntity<?> getReportsForTask(
            @PathVariable UUID wbsItemId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (getUserId(authorization) == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(dailyReportService.getReportsForTask(wbsItemId));
    }

    @GetMapping("/assigned-tasks")
    public ResponseEntity<?> getAssignedTasksForCaptain(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UUID userId = getUserId(authorization);
        if (userId == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(dailyReportService.getAssignedTasksForCaptain(userId, date));
    }

    private UUID getUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        return authTokenService.getUserIdFromToken(token);
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Token không hợp lệ hoặc đã hết hạn"));
    }

    public record ApiResponse(boolean success, String message) {}
}
