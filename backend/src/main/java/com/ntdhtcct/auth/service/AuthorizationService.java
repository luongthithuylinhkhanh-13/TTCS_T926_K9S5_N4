package com.ntdhtcct.auth.service;

import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.ProjectRepository;
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

    public AuthorizationService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {

        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public String checkProjectAccess(
            Long projectId,
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
            throw new UnauthorizedException(
                    "Người dùng không có quyền truy cập công trình này"
            );
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
        }

        throw new ForbiddenException(
                "Người dùng không có vai trò phù hợp để thực hiện thao tác này"
        );
    }
}