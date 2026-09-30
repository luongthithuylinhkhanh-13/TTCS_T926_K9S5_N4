package com.ntdhtcct.service.impl;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.dto.AddMemberRequest;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.dto.UpdateMemberRoleRequest;
import com.ntdhtcct.entity.Project;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.ProjectRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import com.ntdhtcct.service.ProjectMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T-04.4: Triển khai logic gán và cập nhật Role
 * cho User trong Project.
 */
@Service
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectMemberServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMemberServiceImpl(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    public ProjectMemberResponse addMemberToProject(
            Long projectId,
            AddMemberRequest request
    ) {
        log.info(
                "Thêm user {} vào project {} với role {}",
                request.getUserId(),
                projectId,
                request.getRoleCode()
        );

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "PROJECT_NOT_FOUND",
                                "Không tìm thấy dự án với ID: " + projectId
                        )
                );

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "USER_NOT_FOUND",
                                "Không tìm thấy người dùng với ID: "
                                        + request.getUserId()
                        )
                );

        Role role = roleRepository
                .findByName(request.getRoleCode().trim().toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ROLE_NOT_FOUND",
                                "Vai trò không tồn tại: "
                                        + request.getRoleCode()
                        )
                );

        Optional<ProjectMember> existingMemberOpt =
                projectMemberRepository.findByProjectIdAndUserId(
                        projectId,
                        user.getId()
                );

        ProjectMember memberToSave;

        if (existingMemberOpt.isPresent()) {

            ProjectMember existing = existingMemberOpt.get();

            if ("ACTIVE".equalsIgnoreCase(existing.getStatus())) {
                throw new BadRequestException(
                        "MEMBER_ALREADY_EXISTS",
                        "Người dùng đã là thành viên chính thức "
                                + "của dự án này"
                );
            }

            // Kích hoạt lại thành viên cũ đã bị vô hiệu hóa
            existing.setRole(role);
            existing.setStatus("ACTIVE");
            existing.setJoinedAt(OffsetDateTime.now());

            memberToSave = existing;

        } else {
            memberToSave = new ProjectMember(
                    project,
                    user,
                    role
            );
        }

        ProjectMember saved =
                projectMemberRepository.save(memberToSave);

        return ProjectMemberResponse.fromEntity(saved);
    }

    @Override
    public ProjectMemberResponse updateMemberRole(
            Long projectId,
            UUID userId,
            UpdateMemberRoleRequest request
    ) {
        log.info(
                "Cập nhật vai trò user {} trong project {} thành {}",
                userId,
                projectId,
                request.getNewRoleName()
        );

        ProjectMember member =
                projectMemberRepository.findByProjectIdAndUserId(
                        projectId,
                        userId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "MEMBER_NOT_FOUND",
                                "Người dùng [ID=" + userId
                                        + "] không thuộc thành viên "
                                        + "của dự án [ID=" + projectId + "]"
                        )
                );

        Role newRole = roleRepository
                .findByName(
                        request.getNewRoleName()
                                .trim()
                                .toUpperCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ROLE_NOT_FOUND",
                                "Vai trò không tồn tại: "
                                        + request.getNewRoleName()
                        )
                );

        member.setRole(newRole);
        member.setStatus("ACTIVE");

        ProjectMember updated =
                projectMemberRepository.save(member);

        return ProjectMemberResponse.fromEntity(updated);
    }

    @Override
    public void removeMemberFromProject(
            Long projectId,
            UUID userId
    ) {
        log.info(
                "Xóa thành viên user {} khỏi project {}",
                userId,
                projectId
        );

        ProjectMember member =
                projectMemberRepository.findByProjectIdAndUserId(
                        projectId,
                        userId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "MEMBER_NOT_FOUND",
                                "Người dùng [ID=" + userId
                                        + "] không thuộc dự án [ID="
                                        + projectId + "]"
                        )
                );

        projectMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(
            Long projectId
    ) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "PROJECT_NOT_FOUND",
                    "Không tìm thấy dự án với ID: " + projectId
            );
        }

        return projectMemberRepository
                .findByProjectId(projectId)
                .stream()
                .filter(m ->
                        "ACTIVE".equalsIgnoreCase(m.getStatus())
                )
                .map(ProjectMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMemberResponse getProjectMember(
            Long projectId,
            UUID userId
    ) {
        ProjectMember member =
                projectMemberRepository.findByProjectIdAndUserId(
                        projectId,
                        userId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "MEMBER_NOT_FOUND",
                                "Người dùng [ID=" + userId
                                        + "] không thuộc dự án [ID="
                                        + projectId + "]"
                        )
                );

        return ProjectMemberResponse.fromEntity(member);
    }
}
