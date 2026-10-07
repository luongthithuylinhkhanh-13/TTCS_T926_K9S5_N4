package com.ntdhtcct.domain.milestone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectIdOrderByTargetDateAsc(Long projectId);

    List<Milestone> findByCategoryId(UUID categoryId);

    Optional<Milestone> findByIdAndProjectId(UUID id, Long projectId);

    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}