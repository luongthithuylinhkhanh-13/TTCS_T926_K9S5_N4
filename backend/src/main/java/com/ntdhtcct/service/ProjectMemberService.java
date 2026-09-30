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
            UUID projectId,
            AddMemberRequest request
    );

    ProjectMemberResponse updateMemberRole(
            UUID projectId,
            UUID userId,
            UpdateMemberRoleRequest request
    );

    void removeMemberFromProject(
            UUID projectId,
            UUID userId
    );

    List<ProjectMemberResponse> getProjectMembers(
            UUID projectId
    );

    ProjectMemberResponse getProjectMember(
            UUID projectId,
            UUID userId
    );
}
