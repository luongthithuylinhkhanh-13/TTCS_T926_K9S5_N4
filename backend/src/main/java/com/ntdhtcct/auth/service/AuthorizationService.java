package com.ntdhtcct.auth.service;

import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorizationService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthorizationService.class);
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final com.ntdhtcct.repository.UserRepository userRepository;
    private final String accountEmail;
    private final String globalRole;

    public AuthorizationService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            com.ntdhtcct.repository.UserRepository userRepository,
            @org.springframework.beans.factory.annotation.Value("${global.viewer.email:lanc5676@gmail.com}") String accountEmail,
            @org.springframework.beans.factory.annotation.Value("${global.viewer.role:PROJECT_MANAGER}") String globalRole) {

        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.accountEmail = accountEmail;
        this.globalRole = globalRole;
    }

    public String checkProjectAccess(
            UUID projectId,
            UUID userId,
            String[] requiredRoles) {

        log.debug(
                "Checking project access: projectId={}, userId={}, requiredRoles={}",
                projectId,
                userId,
                Arrays.toString(requiredRoles)
        );

        // Kiểm tra project có tồn tại không
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy công trình với ID: " + projectId
            );
        }

        // Kiểm tra user có được phân quyền trong project không
        Optional<String> activeRoleOpt =
                projectMemberRepository.findActiveRoleNameByProjectAndUser(
                        projectId,
                        userId
                );

        if (activeRoleOpt.isEmpty()) {
            if (canViewAllProjects(userId)) {
                activeRoleOpt = Optional.of(globalRole);
            } else {
                throw new ForbiddenException(
                        "Người dùng không phải là thành viên của dự án này"
                );
            }
        }

        String userRole = activeRoleOpt.get();

        // ADMIN được phép truy cập
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return userRole;
        }

        // Kiểm tra role có nằm trong danh sách role được phép không
        if (requiredRoles != null) {
            for (String allowedRole : requiredRoles) {
                if (allowedRole != null
                        && allowedRole.trim().equalsIgnoreCase(userRole)) {
                    return userRole;
                }
            }
            // Allow global viewer to access VIEWER-only routes
            if (canViewAllProjects(userId)) {
                for (String allowedRole : requiredRoles) {
                    if ("VIEWER".equalsIgnoreCase(allowedRole)) {
                        return "VIEWER";
                    }
                }
            }
        }

        throw new ForbiddenException(
                "INSUFFICIENT_PROJECT_ROLE",
                "Người dùng không có quyền thực hiện hành động này"
        );
    }

    public boolean canViewAllProjects(UUID userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(user -> accountEmail.equalsIgnoreCase(user.getEmail()))
                .orElse(false);
    }
}