package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.dto.ProjectMemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class TeamAssignmentController {

    private final TeamAssignmentService teamAssignmentService;
    private final AuthTokenService authTokenService;

    public TeamAssignmentController(
            TeamAssignmentService teamAssignmentService,
            AuthTokenService authTokenService
    ) {
        this.teamAssignmentService = teamAssignmentService;
        this.authTokenService = authTokenService;
    }

    @GetMapping("/crew-members")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<?> getProjectCrews(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        authenticatedUserId(authorization, userIdHeader);
        List<ProjectMemberResponse> crews =
                teamAssignmentService.getProjectCrews(projectId);
        return ResponseEntity.ok(ApiResponse.ok(crews));
    }

    @PutMapping("/tasks/{taskId}/team-assignment")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<?> assignCrew(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody TeamAssignmentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID actorId = authenticatedUserId(authorization, userIdHeader);
        TeamAssignmentService.AssignmentResult result =
                teamAssignmentService.assignCrew(
                        projectId,
                        taskId,
                        request.teamMemberId(),
                        actorId
                );
        return ResponseEntity.ok(ApiResponse.ok(
                result.changed()
                        ? "Đã cập nhật đội thi công được giao."
                        : "Công việc đã được giao cho đội này.",
                result
        ));
    }

    @GetMapping("/tasks/{taskId}/team-assignment/history")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<?> getAssignmentHistory(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        authenticatedUserId(authorization, userIdHeader);
        return ResponseEntity.ok(ApiResponse.ok(
                teamAssignmentService.getAssignmentHistory(projectId, taskId)
        ));
    }

    private UUID authenticatedUserId(String authorization, String userIdHeader) {
        String token = extractToken(authorization);
        if (token == null || !authTokenService.isTokenValid(token)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ hoặc đã hết hạn."
            );
        }
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Thiếu định danh người dùng."
            );
        }

        UUID headerUserId;
        try {
            headerUserId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Định danh người dùng không hợp lệ."
            );
        }

        UUID tokenUserId = authTokenService.getUserIdFromToken(token);
        if (!tokenUserId.equals(headerUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản trong token không khớp người dùng yêu cầu."
            );
        }
        return tokenUserId;
    }

    private String extractToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
    }

    public record TeamAssignmentRequest(UUID teamMemberId) {
    }
}
