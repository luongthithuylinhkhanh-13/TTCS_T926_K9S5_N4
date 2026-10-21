package com.ntdhtcct.domain.milestone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectIdOrderByTargetDateAsc(UUID projectId);

    List<Milestone> findByCategoryId(UUID categoryId);

    Optional<Milestone> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
