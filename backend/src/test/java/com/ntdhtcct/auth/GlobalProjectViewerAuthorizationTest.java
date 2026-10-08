package com.ntdhtcct.auth;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalProjectViewerAuthorizationTest {

    private static final String ACCOUNT_EMAIL = "lanc5676@gmail.com";

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthorizationService authorizationService =
            new AuthorizationService(
                    projectRepository,
                    projectMemberRepository,
                    userRepository,
                    ACCOUNT_EMAIL,
                    "PROJECT_MANAGER"
            );

    private final UUID projectId = UUID.randomUUID();
    private final UUID viewerId = UUID.randomUUID();
    private User viewer;

    @BeforeEach
    void setUp() {
        viewer = new User(ACCOUNT_EMAIL, "password", "Lanç");
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findActiveRoleNameByProjectAndUser(
                projectId,
                viewerId
        )).thenReturn(Optional.empty());
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
    }

    @Test
    void grantsConfiguredAccountProjectManagerAccessWithoutMembership() {
        String role = authorizationService.checkProjectAccess(
                projectId,
                viewerId,
                new String[]{"ADMIN", "PROJECT_MANAGER"}
        );

        assertThat(role).isEqualTo("PROJECT_MANAGER");
    }

    @Test
    void onlyConfiguredGlobalProjectManagerCanViewEveryProject() {
        assertThat(authorizationService.canViewAllProjects(viewerId)).isTrue();

        viewer.setEmail("someone-else@example.com");
        assertThat(authorizationService.canViewAllProjects(viewerId)).isFalse();
        assertThat(authorizationService.canViewAllProjects(null)).isFalse();
    }

    @Test
    void allowsConfiguredAccountToReadViewerOnlyRoutes() {
        String role = authorizationService.checkProjectAccess(
                projectId,
                viewerId,
                new String[]{"VIEWER"}
        );

        assertThat(role).isEqualTo("VIEWER");
    }

    @Test
    void doesNotGrantConfiguredAccountAccessWhenRoleIsNotAllowed() {
        assertThatThrownBy(() -> authorizationService.checkProjectAccess(
                projectId,
                viewerId,
                new String[]{"SITE_ENGINEER"}
        )).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void doesNotGrantGlobalReadAccessToAnotherAccount() {
        viewer.setEmail("someone-else@example.com");

        assertThatThrownBy(() -> authorizationService.checkProjectAccess(
                projectId,
                viewerId,
                new String[]{"VIEWER"}
        )).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void preservesTheExistingProjectRoleForProjectMembers() {
        when(projectMemberRepository.findActiveRoleNameByProjectAndUser(
                projectId,
                viewerId
        )).thenReturn(Optional.of("PROJECT_MANAGER"));

        String role = authorizationService.checkProjectAccess(
                projectId,
                viewerId,
                new String[]{"ADMIN", "PROJECT_MANAGER"}
        );

        assertThat(role).isEqualTo("PROJECT_MANAGER");
    }
}
