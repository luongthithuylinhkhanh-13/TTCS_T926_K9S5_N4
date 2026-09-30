package com.ntdhtcct.auth.interceptor;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.config.RoutePermissionConfig;
import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProjectAuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectAuthorizationInterceptor.class);

    private final AuthorizationService authorizationService;
    private final RoutePermissionConfig routePermissionConfig;

    public ProjectAuthorizationInterceptor(
            AuthorizationService authorizationService,
            RoutePermissionConfig routePermissionConfig) {

        this.authorizationService = authorizationService;
        this.routePermissionConfig = routePermissionConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Long projectId = extractProjectId(request);

        if (projectId == null
                && !request.getRequestURI().startsWith("/api/projects")) {
            return true;
        }

        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new UnauthorizedException(
                    "AUTH_MISSING_USER_ID",
                    "Yêu cầu cung cấp định danh người dùng qua Header X-User-Id"
            );
        }

        UUID userId;

        try {
            userId = UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException(
                    "AUTH_INVALID_USER_ID",
                    "Giá trị Header X-User-Id không hợp lệ: " + userIdHeader
            );
        }

        String[] requiredRoles =
                resolveRequiredRoles(handlerMethod, request);

        String activeRole =
                authorizationService.checkProjectAccess(
                        projectId,
                        userId,
                        requiredRoles
                );

        UserSecurityContext.setUserId(userId);
        UserSecurityContext.setUserProjectRole(activeRole);

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        UserSecurityContext.clear();
    }

    @SuppressWarnings("unchecked")
    private Long extractProjectId(HttpServletRequest request) {

        Object uriTemplateVarsObj =
                request.getAttribute(
                        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
                );

        if (uriTemplateVarsObj instanceof Map<?, ?> uriVars) {

            Object projectIdVal = uriVars.get("projectId");

            if (projectIdVal != null) {
                try {
                    return Long.parseLong(projectIdVal.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String headerProjectId =
                request.getHeader("X-Project-Id");

        if (headerProjectId != null
                && !headerProjectId.isBlank()) {

            try {
                return Long.parseLong(headerProjectId.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String paramProjectId =
                request.getParameter("projectId");

        if (paramProjectId != null
                && !paramProjectId.isBlank()) {

            try {
                return Long.parseLong(paramProjectId.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private String[] resolveRequiredRoles(
            HandlerMethod handlerMethod,
            HttpServletRequest request) {

        RequireProjectRole methodAnnotation =
                handlerMethod.getMethodAnnotation(
                        RequireProjectRole.class
                );

        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        RequireProjectRole classAnnotation =
                AnnotationUtils.findAnnotation(
                        handlerMethod.getBeanType(),
                        RequireProjectRole.class
                );

        if (classAnnotation != null) {
            return classAnnotation.value();
        }

        Optional<String[]> routeRoles =
                routePermissionConfig.findRequiredRoles(
                        request.getMethod(),
                        request.getRequestURI()
                );

        if (routeRoles.isPresent()) {
            return routeRoles.get();
        }

        log.warn(
                "Default Deny kích hoạt cho route [{}] {}",
                request.getMethod(),
                request.getRequestURI()
        );

        throw new ForbiddenException(
                "DEFAULT_DENY",
                "Cơ chế bảo vệ Default Deny: Tuyến đường ["
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
                        + "] chưa được cấp phép truy cập công khai. "
                        + "Truy cập bị từ chối mặc định."
        );
    }
}