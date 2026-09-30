package com.ntdhtcct.controller;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.dto.AddMemberRequest;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.dto.UpdateMemberRoleRequest;
import com.ntdhtcct.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
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
            @PathVariable Long projectId,
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

    /**
     * Cập nhật Role của User trong Project.
     * Yêu cầu ADMIN hoặc PROJECT_MANAGER.
     */
    @PutMapping("/{userId}/role")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable Long projectId,
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
            @PathVariable Long projectId,
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
            @PathVariable Long projectId) {

        List<ProjectMemberResponse> members =
                projectMemberService.getProjectMembers(projectId);

        return ResponseEntity.ok(
                ApiResponse.ok(members)
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
            @PathVariable Long projectId,
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
}