package com.ntdhtcct.repository;

import com.ntdhtcct.domain.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findBySuccessorId(UUID successorId);
    List<TaskDependency> findByPredecessorId(UUID predecessorId);
    List<TaskDependency> findByPredecessorIdInAndSuccessorIdIn(
            Collection<UUID> predecessorIds,
            Collection<UUID> successorIds
    );
    boolean existsByPredecessorIdAndSuccessorId(UUID predecessorId, UUID successorId);
    void deleteByPredecessorIdAndSuccessorId(UUID predecessorId, UUID successorId);
}
