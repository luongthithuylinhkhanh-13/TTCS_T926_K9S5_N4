package com.ntdhtcct.repository;

import com.ntdhtcct.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(
            Long projectId,
            UUID userId
    );

    Optional<ProjectMember> findByProjectIdAndUserIdAndStatus(
            Long projectId,
            UUID userId,
            String status
    );

    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByUserId(UUID userId);

    boolean existsByProjectIdAndUserIdAndStatus(
            Long projectId,
            UUID userId,
            String status
    );

    void deleteByProjectIdAndUserId(
            Long projectId,
            UUID userId
    );

    @Query("""
        SELECT pm.role.name
        FROM ProjectMember pm
        WHERE pm.project.id = :projectId
          AND pm.user.id = :userId
          AND pm.status = 'ACTIVE'
    """)
    Optional<String> findActiveRoleNameByProjectAndUser(
            @Param("projectId") Long projectId,
            @Param("userId") UUID userId
    );
}
