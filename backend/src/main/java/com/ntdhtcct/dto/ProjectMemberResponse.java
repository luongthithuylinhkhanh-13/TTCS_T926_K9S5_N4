package com.ntdhtcct.dto;

import com.ntdhtcct.entity.ProjectMember;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * T-04.4: DTO thông tin thành viên dự án và vai trò được gán.
 */
public class ProjectMemberResponse {

    private Long id;
    private Long projectId;
    private String projectCode;
    private String projectName;

    private UUID userId;
    private String username;
    private String fullName;
    private String email;

    private String roleCode;
    private String roleName;

    private String status;
    private OffsetDateTime joinedAt;

    public ProjectMemberResponse() {
    }

    public static ProjectMemberResponse fromEntity(ProjectMember member) {
        ProjectMemberResponse res = new ProjectMemberResponse();

        res.setId(member.getId());

        if (member.getProject() != null) {
            res.setProjectId(member.getProject().getId());
            res.setProjectCode(member.getProject().getCode());
            res.setProjectName(member.getProject().getName());
        }

        if (member.getUser() != null) {
            res.setUserId(member.getUser().getId());

            // User mới không còn username.
            // Dùng email làm tên đăng nhập.
            res.setUsername(member.getUser().getEmail());

            res.setFullName(member.getUser().getFullName());
            res.setEmail(member.getUser().getEmail());
        }

        if (member.getRole() != null) {
            // Role mới không còn code.
            // Dùng name làm roleCode và roleName.
            res.setRoleCode(member.getRole().getName());
            res.setRoleName(member.getRole().getName());
        }

        res.setStatus(member.getStatus());
        res.setJoinedAt(member.getJoinedAt());

        return res;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
