package com.ntdhtcct.controller;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.dto.AddMemberRequest;
import com.ntdhtcct.dto.AvailableUserResponse;
import com.ntdhtcct.dto.InviteProjectMemberRequest;
import com.ntdhtcct.dto.ProjectMemberInviteResponse;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.dto.UpdateMemberRoleRequest;
import com.ntdhtcct.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController("rbacProjectMemberController")
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    /**
     * Thêm thành viên vào Project.
          * Yêu cầu ADMIN hoặc PROJECT_MANAGER.
     */
    @PostMapping
        @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddMemberRequest request) {

        ProjectMemberResponse response =
                projectMemberService.addMemberToProject(
                        projectId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Gán vai trò và thêm thành viên vào dự án thành công",
                                response
                        )
                );
    }

    @PostMapping("/invitations")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<ProjectMemberInviteResponse>> inviteMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteProjectMemberRequest request) {

        ProjectMemberInviteResponse response =
                projectMemberService.inviteMemberByEmail(projectId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "ADDED".equals(response.status())
                                ? "Người dùng đã được thêm vào dự án"
                                : "Lời mời đã được tạo",
                        response
                ));
    }

    /**
     * Cập nhật Role của User trong Project.
          * Yêu cầu ADMIN hoặc PROJECT_MANAGER.
     */
    @PutMapping("/{userId}/role")
        @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {

        ProjectMemberResponse response =
                projectMemberService.updateMemberRole(
                        projectId,
                        userId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật vai trò thành viên thành công",
                        response
                )
        );
    }

    /**
     * Xóa thành viên khỏi Project.
          * Yêu cầu ADMIN hoặc PROJECT_MANAGER.
     */
    @DeleteMapping("/{userId}")
        @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {

        projectMemberService.removeMemberFromProject(
                projectId,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Xóa thành viên khỏi dự án thành công",
                        null
                )
        );
    }

    /**
     * Lấy danh sách thành viên của Project.
     */
    @GetMapping
    @RequireProjectRole({
            "ADMIN",
            "PROJECT_MANAGER",
            "SITE_ENGINEER",
            "VIEWER"
    })
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable UUID projectId) {

        List<ProjectMemberResponse> members =
                projectMemberService.getProjectMembers(projectId);

        return ResponseEntity.ok(
                ApiResponse.ok(members)
        );
    }

    /**
     * Lấy vai trò của chính người dùng hiện tại trong Project.
     * Cho phép mọi vai trò thành viên dự án.
     */
    @GetMapping("/me")
    @RequireProjectRole({
            "ADMIN",
            "PROJECT_MANAGER",
            "SITE_MANAGER",
            "SITE_ENGINEER",
            "WORKER",
            "STAFF",
            "CUSTOMER",
            "VIEWER"
    })
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> getMyProjectMembership(
            @PathVariable UUID projectId) {

        UUID currentUserId = com.ntdhtcct.auth.context.UserSecurityContext.getUserId();
        ProjectMemberResponse member =
                projectMemberService.getProjectMember(
                        projectId,
                        currentUserId
                );

        return ResponseEntity.ok(
                ApiResponse.ok(member)
        );
    }

    /**
     * Xem chi tiết một thành viên trong Project.
     */
    @GetMapping("/{userId}")
    @RequireProjectRole({
            "ADMIN",
            "PROJECT_MANAGER",
            "SITE_ENGINEER",
            "VIEWER"
    })
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> getProjectMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {

        ProjectMemberResponse member =
                projectMemberService.getProjectMember(
                        projectId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.ok(member)
        );
    }

    /**
     * Lấy danh sách tài khoản ACTIVE hợp lệ để thêm vào dự án.
     * Yêu cầu ADMIN hoặc PROJECT_MANAGER.
     */
    @GetMapping("/available-users")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<List<AvailableUserResponse>>> getAvailableUsers(
            @PathVariable UUID projectId) {

        List<AvailableUserResponse> users =
                projectMemberService.getAvailableUsers(projectId);

        return ResponseEntity.ok(
                ApiResponse.ok(users)
        );
    }
}