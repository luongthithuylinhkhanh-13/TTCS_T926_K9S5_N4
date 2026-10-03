package com.ntdhtcct.service;

import com.ntdhtcct.domain.DependencyType;
import com.ntdhtcct.domain.TaskDependency;
import com.ntdhtcct.dto.CreateDependencyRequest;
import com.ntdhtcct.repository.TaskDependencyRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public class TaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;

    public TaskDependencyService(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    @Transactional
    public TaskDependency addDependency(UUID successorId, CreateDependencyRequest request) {
        UUID predecessorId = request.getPredecessorId();
        DependencyType type = request.getDependencyType() != null ? request.getDependencyType() : DependencyType.FS;
        Integer lag = request.getLagDays() != null ? request.getLagDays() : 0;

        if (predecessorId.equals(successorId)) {
            throw new IllegalArgumentException("Một công việc không thể tự phụ thuộc vào chính nó.");
        }

        if (dependencyRepository.existsByPredecessorIdAndSuccessorId(predecessorId, successorId)) {
            throw new IllegalArgumentException("Quan hệ phụ thuộc giữa 2 công việc này đã tồn tại.");
        }

        if (isReachable(successorId, predecessorId)) {
            throw new IllegalArgumentException("Không thể thiết lập quan hệ vì sẽ tạo thành vòng lặp tuần hoàn (Cyclic Dependency).");
        }

        TaskDependency dependency = new TaskDependency(predecessorId, successorId, type, lag);
        return dependencyRepository.save(dependency);
    }

    public boolean isReachable(UUID startTaskId, UUID targetTaskId) {
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();

        queue.add(startTaskId);
        visited.add(startTaskId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (current.equals(targetTaskId)) {
                return true;
            }

            List<TaskDependency> outgoingDeps = dependencyRepository.findByPredecessorId(current);
            for (TaskDependency dep : outgoingDeps) {
                UUID nextNode = dep.getSuccessorId();
                if (!visited.contains(nextNode)) {
                    visited.add(nextNode);
                    queue.add(nextNode);
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<TaskDependency> getDependencies(UUID taskId) {
        return dependencyRepository.findBySuccessorId(taskId);
    }

    @Transactional
    public void deleteDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new IllegalArgumentException("Quan hệ phụ thuộc không tồn tại với ID: " + dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }
}