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
        addRule(HttpMethod.POST, "/api/projects/*/members", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.POST, "/api/projects/*/members/invitations", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.PUT, "/api/projects/*/members/*/role", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.DELETE, "/api/projects/*/members/*", new String[]{"ADMIN", "PROJECT_MANAGER"});

        // Xem danh sách thành viên dự án: Mọi thành viên có vai trò trong dự án
        addRule(HttpMethod.GET, "/api/projects/*/members", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/members/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "VIEWER"});

        // Route xem thông tin dự án
        addRule(HttpMethod.GET, "/api/projects/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});

        // WBS, schedule and critical-path read routes
        addRule(HttpMethod.GET, "/api/projects/*/wbs", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/schedule", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/critical-path", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/categories/*/tasks", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.POST, "/api/projects/*/categories/*/tasks", new String[]{"ADMIN", "PROJECT_MANAGER"});

        // WBS task creation is managed; task updates also allow field engineers to record progress.
        addRule(HttpMethod.POST, "/api/projects/*/wbs", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"});
        addRule(HttpMethod.POST, "/api/projects/*/tasks", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.PUT, "/api/projects/*/wbs/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"});
        addRule(HttpMethod.PUT, "/api/projects/*/tasks/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"});
        addRule(HttpMethod.DELETE, "/api/projects/*/wbs/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"});

        // T-43, T-44, T-45 (NTDHTCT-166): Milestones và cảnh báo mốc tiến độ
        addRule(HttpMethod.GET, "/api/projects/*/milestones", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/milestones/*", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/milestones/warnings", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.GET, "/api/projects/*/milestone-warnings", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.POST, "/api/projects/*/milestones", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.PUT, "/api/projects/*/milestones/*", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.DELETE, "/api/projects/*/milestones/*", new String[]{"ADMIN", "PROJECT_MANAGER"});

        addRule(HttpMethod.GET, "/api/projects/*/crew-members", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
        addRule(HttpMethod.PUT, "/api/projects/*/tasks/*/team-assignment", new String[]{"ADMIN", "PROJECT_MANAGER"});
        addRule(HttpMethod.GET, "/api/projects/*/tasks/*/team-assignment/history", new String[]{"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"});
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
