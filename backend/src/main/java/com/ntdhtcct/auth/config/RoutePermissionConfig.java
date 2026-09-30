package com.ntdhtcct.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * T-04.8: Cấu hình phân quyền động theo từng Route (URL Pattern + HTTP Method).
 * T-04.9: Hỗ trợ cơ chế Default Deny.
 */
@Configuration
public class RoutePermissionConfig {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<RouteRule> rules = new ArrayList<>();

    public RoutePermissionConfig() {
        initDefaultRules();
    }

    private void initDefaultRules() {
        // Quản lý thành viên: Thêm, sửa role, xóa thành viên yêu cầu ADMIN hoặc PROJECT_MANAGER
        addRule(HttpMethod.POST, "/api/projects/*/members", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.PUT, "/api/projects/*/members/*/role", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.DELETE, "/api/projects/*/members/*", new String[]{"ADMIN", "PROJECT_MANAGER"});
        
        // Xem danh sách thành viên dự án: Mọi thành viên có vai trò trong dự án
        addRule(HttpMethod.GET, "/api/projects/*/members", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/members/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "VIEWER"});

        // Route xem thông tin dự án
        addRule(HttpMethod.GET, "/api/projects/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
    }

    public void addRule(HttpMethod method, String pathPattern, String[] requiredRoles) {
        rules.add(new RouteRule(method, pathPattern, requiredRoles));
    }

    /**
     * Tìm quy tắc quyền theo HTTP Method và đường dẫn Request.
     */
    public Optional<String[]> findRequiredRoles(String httpMethod, String requestPath) {
        for (RouteRule rule : rules) {
            if ((rule.method() == null || rule.method().name().equalsIgnoreCase(httpMethod))
                    && pathMatcher.match(rule.pathPattern(), requestPath)) {
                return Optional.of(rule.requiredRoles());
            }
        }
        return Optional.empty();
    }

    public record RouteRule(HttpMethod method, String pathPattern, String[] requiredRoles) {
    }
}
