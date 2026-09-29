package com.ntdhtcct.repository;

import com.ntdhtcct.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * T-19 (NTDHTCT-145): Repository truy vấn dữ liệu công việc (Task).
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    Optional<Task> findByProjectIdAndCode(Long projectId, String code);

    boolean existsByProjectIdAndCode(Long projectId, String code);

    void deleteByProjectId(Long projectId);
}
