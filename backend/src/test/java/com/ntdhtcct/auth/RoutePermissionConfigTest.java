package com.ntdhtcct.auth;

import com.ntdhtcct.auth.config.RoutePermissionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePermissionConfigTest {

    private final RoutePermissionConfig routePermissionConfig = new RoutePermissionConfig();

    @Test
    void allowsFieldEngineersToUpdateWbsTasks() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.PUT.name(),
                        "/api/projects/123/tasks/456"
                )
                .orElseThrow();

        assertThat(roles).contains("ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER");
        assertThat(roles).doesNotContain("WORKER", "VIEWER");
    }

    @Test
    void restrictsWbsTaskCreationToProjectManagers() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.POST.name(),
                        "/api/projects/123/tasks"
                )
                .orElseThrow();

        assertThat(roles).containsExactly("ADMIN", "PROJECT_MANAGER");
    }

    @Test
    void allowsEngineersAndManagersToCreateDiaryEntries() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.POST.name(),
                        "/api/projects/123/diary-entries"
                )
                .orElseThrow();

        assertThat(roles).contains("ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER");
        assertThat(roles).doesNotContain("WORKER", "VIEWER");
    }

    @Test
    void allowsProjectMembersToViewDiaryImages() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.GET.name(),
                        "/api/projects/123/diary-entries/456/images"
                )
                .orElseThrow();

        assertThat(roles).contains("ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER");
    }
}
