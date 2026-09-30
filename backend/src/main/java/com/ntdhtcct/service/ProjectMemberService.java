package com.ntdhtcct.service;

import com.ntdhtcct.dto.AddMemberRequest;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.dto.UpdateMemberRoleRequest;

import java.util.List;
import java.util.UUID;

/**
 * T-04.4: Service quản lý thành viên và gán vai trò (RBAC)
 * cho người dùng trong dự án.
 */
public interface ProjectMemberService {

    ProjectMemberResponse addMemberToProject(
            Long projectId,
            AddMemberRequest request
    );

    ProjectMemberResponse updateMemberRole(
            Long projectId,
            UUID userId,
            UpdateMemberRoleRequest request
    );

    void removeMemberFromProject(
            Long projectId,
            UUID userId
    );

    List<ProjectMemberResponse> getProjectMembers(
            Long projectId
    );

    ProjectMemberResponse getProjectMember(
            Long projectId,
            UUID userId
    );
}
