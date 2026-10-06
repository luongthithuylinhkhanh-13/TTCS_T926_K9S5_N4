package com.ntdhtcct.domain.project;

import com.ntdhtcct.domain.user.User;
import com.ntdhtcct.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectInvitationRepository projectInvitationRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectInvitationRepository = projectInvitationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public void addMemberOrInvite(UUID projectId, String email, String role) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found");
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
                throw new IllegalArgumentException("User is already a member of this project");
            }
            ProjectMember member = new ProjectMember(projectId, user.getId(), role);
            projectMemberRepository.save(member);
        } else {
            // Check if there is already a pending invitation
            Optional<ProjectInvitation> existingInvite = projectInvitationRepository.findByProjectIdAndEmailAndStatus(projectId, email, "PENDING");
            if (existingInvite.isPresent()) {
                throw new IllegalArgumentException("An invitation has already been sent to this email");
            }
            
            // Create invitation
            String token = UUID.randomUUID().toString();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7); // Valid for 7 days
            ProjectInvitation invite = new ProjectInvitation(projectId, email, role, token, "PENDING", expiresAt);
            projectInvitationRepository.save(invite);
            
            // In a real application, send an email here with the token link
            System.out.println("Mock Email sent to " + email + " with token: " + token);
        }
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> getMembers(UUID projectId) {
        return projectMemberRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
