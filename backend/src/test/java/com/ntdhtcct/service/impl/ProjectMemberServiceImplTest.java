package com.ntdhtcct.service.impl;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectInvitation;
import com.ntdhtcct.domain.project.ProjectInvitationRepository;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.dto.InviteProjectMemberRequest;
import com.ntdhtcct.dto.ProjectMemberInviteResponse;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    private UUID projectId;
    private Project project;
    private Role role;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = mock(Project.class);
        role = mock(Role.class);
        lenient().when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        lenient().when(roleRepository.findByName("SITE_ENGINEER")).thenReturn(Optional.of(role));
    }

    @Test
    void inviteMemberByEmail_CreatesInvitationForUnknownEmail() {
        when(project.getId()).thenReturn(projectId);
        when(role.getName()).thenReturn("SITE_ENGINEER");
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.empty());
        when(projectInvitationRepository.findByProjectIdAndEmailIgnoreCaseAndStatus(
                projectId, "person@example.com", "PENDING"))
                .thenReturn(Optional.empty());

        ProjectMemberInviteResponse result = projectMemberService.inviteMemberByEmail(
                projectId,
                new InviteProjectMemberRequest("  Person@Example.com ", " site_engineer ")
        );

        ArgumentCaptor<ProjectInvitation> invitationCaptor =
                ArgumentCaptor.forClass(ProjectInvitation.class);
        verify(projectInvitationRepository).save(invitationCaptor.capture());
        ProjectInvitation saved = invitationCaptor.getValue();

        assertEquals("INVITED", result.status());
        assertEquals("person@example.com", result.email());
        assertEquals("SITE_ENGINEER", result.roleCode());
        assertNull(result.userId());
        assertEquals(saved.getToken(), result.invitationToken());
        assertDoesNotThrow(() -> UUID.fromString(result.invitationToken()));
        assertEquals("PENDING", saved.getStatus());
        assertEquals(projectId, saved.getProjectId());
        assertEquals("person@example.com", saved.getEmail());
        assertEquals("SITE_ENGINEER", saved.getRole());
        assertTrue(saved.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(6)));
        assertTrue(saved.getExpiresAt().isBefore(OffsetDateTime.now().plusDays(8)));
    }

    @Test
    void inviteMemberByEmail_AddsExistingUserDirectly() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMemberInviteResponse result = projectMemberService.inviteMemberByEmail(
                projectId,
                new InviteProjectMemberRequest("person@example.com", "SITE_ENGINEER")
        );

        assertEquals("ADDED", result.status());
        assertEquals(userId, result.userId());
        assertNull(result.invitationToken());
        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMemberByEmail_RejectsAnUnexpiredPendingInvitation() {
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.empty());
        ProjectInvitation pending = new ProjectInvitation(
                projectId,
                "person@example.com",
                "SITE_ENGINEER",
                UUID.randomUUID().toString(),
                "PENDING",
                OffsetDateTime.now().plusDays(1)
        );
        when(projectInvitationRepository.findByProjectIdAndEmailIgnoreCaseAndStatus(
                projectId, "person@example.com", "PENDING"))
                .thenReturn(Optional.of(pending));

        assertThrows(
                BadRequestException.class,
                () -> projectMemberService.inviteMemberByEmail(
                        projectId,
                        new InviteProjectMemberRequest("person@example.com", "SITE_ENGINEER")
                )
        );

        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMemberByEmail_ExpiresOldInvitationAndCreatesANewOne() {
        when(project.getId()).thenReturn(projectId);
        when(role.getName()).thenReturn("SITE_ENGINEER");
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.empty());
        ProjectInvitation expired = new ProjectInvitation(
                projectId,
                "person@example.com",
                "SITE_ENGINEER",
                UUID.randomUUID().toString(),
                "PENDING",
                OffsetDateTime.now().minusDays(1)
        );
        when(projectInvitationRepository.findByProjectIdAndEmailIgnoreCaseAndStatus(
                projectId, "person@example.com", "PENDING"))
                .thenReturn(Optional.of(expired));

        ProjectMemberInviteResponse result = projectMemberService.inviteMemberByEmail(
                projectId,
                new InviteProjectMemberRequest("person@example.com", "SITE_ENGINEER")
        );

        assertEquals("EXPIRED", expired.getStatus());
        assertEquals("INVITED", result.status());
        verify(projectInvitationRepository, times(2)).save(any(ProjectInvitation.class));
    }

    @Test
    void addMemberToProject_RejectsAdminRole() {
        UUID userId = UUID.randomUUID();
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> projectMemberService.addMemberToProject(
                        projectId,
                        new com.ntdhtcct.dto.AddMemberRequest(userId, "ADMIN")
                )
        );
        assertEquals("INVALID_PROJECT_ROLE", ex.getCode());
    }

    @Test
    void addMemberToProject_RejectsInactiveUser() {
        UUID userId = UUID.randomUUID();
        User pendingUser = mock(User.class);
        when(pendingUser.getStatus()).thenReturn("PENDING");
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> projectMemberService.addMemberToProject(
                        projectId,
                        new com.ntdhtcct.dto.AddMemberRequest(userId, "SITE_ENGINEER")
                )
        );
        assertEquals("USER_NOT_ACTIVE", ex.getCode());
    }

    @Test
    void addMemberToProject_RejectsAdminUser() {
        UUID userId = UUID.randomUUID();
        User admin = mock(User.class);
        when(admin.getStatus()).thenReturn("ACTIVE");
        when(admin.getEmail()).thenReturn("admin@admin.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> projectMemberService.addMemberToProject(
                        projectId,
                        new com.ntdhtcct.dto.AddMemberRequest(userId, "SITE_ENGINEER")
                )
        );
        assertEquals("INVALID_PROJECT_MEMBER", ex.getCode());
    }

    @Test
    void updateMemberRole_RejectsAdminRole() {
        UUID userId = UUID.randomUUID();
        ProjectMember member = mock(ProjectMember.class);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(member));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> projectMemberService.updateMemberRole(
                        projectId,
                        userId,
                        new com.ntdhtcct.dto.UpdateMemberRoleRequest("ADMIN")
                )
        );
        assertEquals("INVALID_PROJECT_ROLE", ex.getCode());
    }

    @Test
    void getAvailableUsers_ReturnsActiveUsersAndFiltersOutAdmin() {
        when(projectRepository.existsById(projectId)).thenReturn(true);

        UUID user1Id = UUID.randomUUID();
        User user1 = mock(User.class);
        when(user1.getId()).thenReturn(user1Id);
        when(user1.getEmail()).thenReturn("worker@site.com");
        when(user1.getFullName()).thenReturn("Worker 1");
        when(user1.getStatus()).thenReturn("ACTIVE");

        User admin = mock(User.class);
        when(admin.getEmail()).thenReturn("admin@admin.com");
        when(admin.getStatus()).thenReturn("ACTIVE");

        User pendingUser = mock(User.class);
        when(pendingUser.getStatus()).thenReturn("PENDING");

        when(userRepository.findAll()).thenReturn(java.util.List.of(user1, admin, pendingUser));
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(java.util.List.of());

        java.util.List<com.ntdhtcct.dto.AvailableUserResponse> availableUsers =
                projectMemberService.getAvailableUsers(projectId);

        assertEquals(1, availableUsers.size());
        assertEquals("worker@site.com", availableUsers.get(0).email());
        assertFalse(availableUsers.get(0).isAlreadyMember());
    }
}
