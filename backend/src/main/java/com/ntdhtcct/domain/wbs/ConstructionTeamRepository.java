package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstructionTeamRepository
        extends JpaRepository<ConstructionTeam, UUID> {

    List<ConstructionTeam> findByProjectId(Long projectId);

    Optional<ConstructionTeam> findByProjectIdAndId(
            Long projectId,
            UUID id
    );

    boolean existsByProjectIdAndCode(
            Long projectId,
            String code
    );
}