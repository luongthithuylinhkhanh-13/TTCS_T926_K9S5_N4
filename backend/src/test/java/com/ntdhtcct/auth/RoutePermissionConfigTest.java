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
    void restrictsCrewReassignmentToProjectManagers() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.PUT.name(),
                        "/api/projects/123/tasks/456/team-assignment"
                )
                .orElseThrow();

        assertThat(roles).containsExactly("ADMIN", "PROJECT_MANAGER");
    }

    @Test
    void restrictsProjectMemberInvitationsToProjectManagers() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.POST.name(),
                        "/api/projects/123/members/invitations"
                )
                .orElseThrow();

        assertThat(roles).containsExactly("ADMIN", "PROJECT_MANAGER");
    }

    @Test
    void allowsSiteEngineersToUpdateSiteDiariesButNotWorkers() {
        String[] roles = routePermissionConfig
                .findRequiredRoles(
                        HttpMethod.PUT.name(),
                        "/api/projects/123/site-diaries/456"
                )
                .orElseThrow();

        assertThat(roles).contains("ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER");
        assertThat(roles).doesNotContain("WORKER", "VIEWER");
    }
}
