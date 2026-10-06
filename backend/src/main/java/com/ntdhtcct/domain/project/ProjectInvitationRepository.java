package com.ntdhtcct.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    List<ProjectInvitation> findByProjectId(UUID projectId);
    Optional<ProjectInvitation> findByToken(String token);
    Optional<ProjectInvitation> findByProjectIdAndEmailAndStatus(UUID projectId, String email, String status);
}
