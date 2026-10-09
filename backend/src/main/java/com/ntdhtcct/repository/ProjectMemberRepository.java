package com.ntdhtcct.repository;

import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.domain.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;

@Primary
@Repository("projectMemberRepository")
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT pm.project FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.status = 'ACTIVE'")
    List<Project> findActiveProjectsByUserId(@Param("userId") UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserId(
            UUID projectId,
            UUID userId
    );

    Optional<ProjectMember> findByProjectIdAndUserIdAndStatus(
            UUID projectId,
            UUID userId,
            String status
    );

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    boolean existsByProjectIdAndUserIdAndStatus(
            UUID projectId,
            UUID userId,
            String status
    );

    void deleteByProjectIdAndUserId(
            UUID projectId,
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
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId
    );
}
