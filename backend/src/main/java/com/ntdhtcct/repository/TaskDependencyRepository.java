package com.ntdhtcct.repository;

import com.ntdhtcct.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * T-18 (NTDHTCT-144): Repository truy vấn quan hệ phụ thuộc công việc (TaskDependency).
 */
@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByProjectId(Long projectId);

    List<TaskDependency> findByPredecessorId(Long predecessorId);

    List<TaskDependency> findBySuccessorId(Long successorId);

    Optional<TaskDependency> findByPredecessorIdAndSuccessorId(Long predecessorId, Long successorId);

    boolean existsByPredecessorIdAndSuccessorId(Long predecessorId, Long successorId);

    void deleteByProjectId(Long projectId);
}
