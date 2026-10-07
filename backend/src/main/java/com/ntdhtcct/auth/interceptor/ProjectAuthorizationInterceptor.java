package com.ntdhtcct.auth.interceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.config.RoutePermissionConfig;
import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.auth.AuthTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProjectAuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectAuthorizationInterceptor.class);

    private final AuthorizationService authorizationService;
    private final RoutePermissionConfig routePermissionConfig;
    private final AuthTokenService authTokenService;

    public ProjectAuthorizationInterceptor(
            AuthorizationService authorizationService,
            RoutePermissionConfig routePermissionConfig,
            AuthTokenService authTokenService) {

        this.authorizationService = authorizationService;
        this.routePermissionConfig = routePermissionConfig;
        this.authTokenService = authTokenService;
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

        if ("GET".equalsIgnoreCase(request.getMethod())
                && "/api/projects".equals(request.getRequestURI())) {
            return true;
        }

        Long projectId = extractProjectId(request);

        if (projectId == null) {
            return true;
        }

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException(
                    "AUTH_MISSING_TOKEN",
                    "Yêu cầu cung cấp token đăng nhập qua Header Authorization"
            );
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty() || !authTokenService.isTokenValid(token)) {
            throw new UnauthorizedException(
                    "AUTH_INVALID_TOKEN",
                    "Token không hợp lệ hoặc đã hết hạn"
            );
        }

        UUID userId = authTokenService.getUserIdFromToken(token);

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
                    return Long.valueOf(projectIdVal.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String requestUri = request.getRequestURI();
        String projectPathPrefix = "/api/projects/";

        if (!requestUri.startsWith(projectPathPrefix)) {
            return null;
        }

        String projectIdValue =
                requestUri.substring(projectPathPrefix.length())
                        .split("/", 2)[0];

        if (projectIdValue.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(projectIdValue);
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "INVALID_PROJECT_ID",
                    "ID công trình không đúng định dạng số"
            );
        }
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