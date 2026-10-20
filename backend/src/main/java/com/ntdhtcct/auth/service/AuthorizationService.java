package com.ntdhtcct.auth.service;

import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthorizationService.class);
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final String globalProjectRoleEmail;
    private final String globalProjectRole;

    public AuthorizationService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            @Value("${app.authorization.global-project-role-email:}")
            String globalProjectRoleEmail,
            @Value("${app.authorization.global-project-role:VIEWER}")
            String globalProjectRole) {

        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.globalProjectRoleEmail = globalProjectRoleEmail;
        this.globalProjectRole = globalProjectRole;
    }

    private Set<String> configuredGlobalProjectRoleEmails() {
        if (globalProjectRoleEmail == null || globalProjectRoleEmail.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(globalProjectRoleEmail.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private boolean hasConfiguredGlobalProjectRoleEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return configuredGlobalProjectRoleEmails().contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public String checkProjectAccess(
            UUID projectId,
            UUID userId,
            String[] requiredRoles) {

        if (projectId == null) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Không tìm thấy công trình.");
        }
        if (userId == null) {
            throw new UnauthorizedException(
                    "AUTH_MISSING_USER_ID",
                    "Không xác định được người dùng yêu cầu."
            );
        }

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

        if (hasGlobalProjectAccess(userId, requiredRoles)) {
            return (requiredRoles == null || globalProjectRoleCanManage(requiredRoles))
                    ? "PROJECT_MANAGER"
                    : "VIEWER";
        }

        // Kiểm tra user có được phân quyền trong project không
        Optional<String> activeRoleOpt =
                projectMemberRepository.findActiveRoleNameByProjectAndUser(
                        projectId,
                        userId
                );

        if (activeRoleOpt.isEmpty()) {
            Optional<com.ntdhtcct.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent() && userOpt.get().getRole() != null && "ADMIN".equalsIgnoreCase(userOpt.get().getRole().getName())) {
                activeRoleOpt = Optional.of("ADMIN");
            } else if (canViewAllProjects(userId)) {
                activeRoleOpt = Optional.of(globalProjectRole);
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

        if (requiredRoles == null || requiredRoles.length == 0) {
            return userRole;
        }

        // Kiểm tra role có nằm trong danh sách role được phép không
        if (requiredRoles != null) {
            for (String allowedRole : requiredRoles) {
                if (allowedRole != null && isRoleAuthorized(userRole, allowedRole)) {
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

    private boolean isRoleAuthorized(String userRole, String allowedRole) {
        if (userRole == null || allowedRole == null) {
            return false;
        }
        if (userRole.trim().equalsIgnoreCase(allowedRole.trim())) {
            return true;
        }
        // SITE_MANAGER (Chỉ huy trưởng) can perform management actions (WBS, task assignment, diary locks)
        if ("PROJECT_MANAGER".equalsIgnoreCase(allowedRole) && "SITE_MANAGER".equalsIgnoreCase(userRole)) {
            return true;
        }
        // Any recognized project role can view if the route requires VIEWER
        if ("VIEWER".equalsIgnoreCase(allowedRole)) {
            return true;
        }
        return false;
    }

    private boolean hasGlobalProjectAccess(
            UUID userId,
            String[] requiredRoles
    ) {
        if (userId == null
                || globalProjectRole == null
                || globalProjectRole.isBlank()) {
            return false;
        }

        boolean configuredAccount = userRepository.findById(userId)
            .map(user -> isConfiguredAccount(user.getEmail()))
                .orElse(false);
        return configuredAccount
                && (requiredRoles == null
                    || globalProjectRoleCanManage(requiredRoles)
                    || (isGlobalProjectManager()
                        && containsRole(requiredRoles, "VIEWER")));
    }

    public boolean canViewAllProjects(UUID userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
            .map(user -> {
                if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
                    return true;
                }
                return isGlobalProjectManager() && isConfiguredAccount(user.getEmail());
            })
            .orElse(false);
    }

    private boolean isConfiguredAccount(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return false;
        }
        String normalized = userEmail.trim().toLowerCase();
        if ("test@test.com".equals(normalized)) {
            return true;
        }
        if (globalProjectRoleEmail != null && !globalProjectRoleEmail.isBlank()) {
            for (String configured : globalProjectRoleEmail.split(",")) {
                if (configured.trim().equalsIgnoreCase(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean globalProjectRoleCanManage(String[] requiredRoles) {
        if (isGlobalAdmin()) {
            return requiredRoles != null && requiredRoles.length > 0;
        }
        return isGlobalProjectManager()
                && containsRole(requiredRoles, "PROJECT_MANAGER");
    }

    private boolean isGlobalProjectManager() {
        return "PROJECT_MANAGER".equalsIgnoreCase(globalProjectRole);
    }

    private boolean isGlobalAdmin() {
        return "ADMIN".equalsIgnoreCase(globalProjectRole);
    }

    private boolean containsRole(String[] roles, String expectedRole) {
        return Arrays.stream(roles)
                .anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }
}