package com.ntdhtcct.auth.interceptor;

import com.ntdhtcct.auth.config.RoutePermissionConfig;
import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.auth.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectAuthorizationInterceptorTest {

    private AuthorizationService authorizationService;
    private RoutePermissionConfig routePermissionConfig;
    private AuthTokenService authTokenService;
    private ProjectAuthorizationInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HandlerMethod handlerMethod;

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String validToken = "valid-jwt-token-xyz";

    static class DummyController {
        public void testEndpoint() {}
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        authorizationService = mock(AuthorizationService.class);
        routePermissionConfig = new RoutePermissionConfig();
        authTokenService = mock(AuthTokenService.class);
        interceptor = new ProjectAuthorizationInterceptor(authorizationService, routePermissionConfig, authTokenService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        Method method = DummyController.class.getMethod("testEndpoint");
        handlerMethod = new HandlerMethod(new DummyController(), method);

        when(request.getRequestURI()).thenReturn("/api/projects/" + projectId + "/diary-entries/entry-1/images/img-1/display");
        when(request.getMethod()).thenReturn("GET");

        when(authTokenService.isTokenValid(validToken)).thenReturn(true);
        when(authTokenService.getUserIdFromToken(validToken)).thenReturn(userId);
        when(authorizationService.checkProjectAccess(eq(projectId), eq(userId), any())).thenReturn("VIEWER");
    }

    @Test
    void preHandle_WithBearerHeader_AuthenticatesSuccessfully() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_WithQueryParamToken_AuthenticatesSuccessfully() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(validToken);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_WithQueryParamAccessToken_AuthenticatesSuccessfully() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);
        when(request.getParameter("access_token")).thenReturn(validToken);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_WithoutTokenOrHeader_ThrowsUnauthorizedException() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);
        when(request.getParameter("access_token")).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Yêu cầu cung cấp token đăng nhập");
    }

    @Test
    void preHandle_WithInvalidTokenInQueryParam_ThrowsUnauthorizedException() {
        String invalidToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(invalidToken);
        when(authTokenService.isTokenValid(invalidToken)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Token không hợp lệ hoặc đã hết hạn");
    }
}
