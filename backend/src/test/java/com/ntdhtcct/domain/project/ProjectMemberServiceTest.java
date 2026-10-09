package com.ntdhtcct.domain.project;

import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private UUID projectId;
    private UUID userId;
    private String email;
    private User mockUser;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        email = "test@example.com";
        mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(userId);
    }

    @Test
    void addMemberOrInvite_ProjectNotFound() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            projectMemberService.addMemberOrInvite(projectId, email, "CAPTAIN");
        });

        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void addMemberOrInvite_UserExists_AlreadyMember() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            projectMemberService.addMemberOrInvite(projectId, email, "CAPTAIN");
        });

        assertEquals("User is already a member of this project", exception.getMessage());
    }

    @Test
    void addMemberOrInvite_UserExists_Success() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);

        projectMemberService.addMemberOrInvite(projectId, email, "CAPTAIN");

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository, times(1)).save(captor.capture());

        ProjectMember savedMember = captor.getValue();
        assertEquals(projectId, savedMember.getProjectId());
        assertEquals(userId, savedMember.getUserId());
        assertEquals("CAPTAIN", savedMember.getRole());
    }

    @Test
    void addMemberOrInvite_UserNotExists_InviteAlreadySent() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        
        ProjectInvitation pendingInvite = new ProjectInvitation();
        when(projectInvitationRepository.findByProjectIdAndEmailAndStatus(projectId, email, "PENDING"))
                .thenReturn(Optional.of(pendingInvite));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            projectMemberService.addMemberOrInvite(projectId, email, "CAPTAIN");
        });

        assertEquals("An invitation has already been sent to this email", exception.getMessage());
    }

    @Test
    void addMemberOrInvite_UserNotExists_SuccessInvite() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(projectInvitationRepository.findByProjectIdAndEmailAndStatus(projectId, email, "PENDING"))
                .thenReturn(Optional.empty());

        projectMemberService.addMemberOrInvite(projectId, email, "CAPTAIN");

        ArgumentCaptor<ProjectInvitation> captor = ArgumentCaptor.forClass(ProjectInvitation.class);
        verify(projectInvitationRepository, times(1)).save(captor.capture());

        ProjectInvitation savedInvite = captor.getValue();
        assertEquals(projectId, savedInvite.getProjectId());
        assertEquals(email, savedInvite.getEmail());
        assertEquals("CAPTAIN", savedInvite.getRole());
        assertEquals("PENDING", savedInvite.getStatus());
        assertNotNull(savedInvite.getToken());
    }

    @Test
    void removeMember_NotMember() {
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            projectMemberService.removeMember(projectId, userId);
        });

        assertEquals("User is not a member of this project", exception.getMessage());
    }

    @Test
    void removeMember_Success() {
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        projectMemberService.removeMember(projectId, userId);

        verify(projectMemberRepository, times(1)).deleteByProjectIdAndUserId(projectId, userId);
    }
}
