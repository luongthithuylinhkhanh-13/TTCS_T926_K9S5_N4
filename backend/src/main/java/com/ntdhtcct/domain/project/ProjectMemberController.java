package com.ntdhtcct.domain.project;

import com.ntdhtcct.domain.auth.AuthTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController("invitationProjectMemberController")
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final AuthTokenService authTokenService;

    public ProjectMemberController(ProjectMemberService projectMemberService, AuthTokenService authTokenService) {
        this.projectMemberService = projectMemberService;
        this.authTokenService = authTokenService;
    }

    @PostMapping
    public ResponseEntity<?> addMember(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UUID userId = getUserId(authorization);
        if (userId == null) {
            return unauthorized();
        }
        
        // In a real app, you would check if 'userId' has the permission to add members (e.g. Project Manager)

        String email = request.get("email");
        String role = request.get("role");

        if (email == null || email.trim().isEmpty() || role == null || role.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Email and role are required"));
        }

        try {
            projectMemberService.addMemberOrInvite(projectId, email, role);
            return ResponseEntity.ok(new ApiResponse(true, "Member added or invited successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UUID currentUserId = getUserId(authorization);
        if (currentUserId == null) {
            return unauthorized();
        }

        // Check if current user has permission (e.g. Project Manager)

        try {
            projectMemberService.removeMember(projectId, memberId);
            return ResponseEntity.ok(new ApiResponse(true, "Member removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMembers(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (getUserId(authorization) == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(projectMemberService.getMembers(projectId));
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
