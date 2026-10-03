package com.ntdhtcct.domain.dependency;

import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskDependencyService {

    private final TaskDependencyRepository repository;
    private final WbsItemRepository wbsItemRepository;

    public TaskDependencyService(TaskDependencyRepository repository, WbsItemRepository wbsItemRepository) {
        this.repository = repository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependenciesByTask(UUID taskId) {
        return repository.findBySuccessorId(taskId)
                .stream()
                .map(TaskDependencyResponse::from)
                .toList();
    }

    @Transactional
    public TaskDependencyResponse addDependency(UUID successorId, CreateDependencyRequest request) {
        UUID predId = request.predecessorId();

        if (predId.equals(successorId)) {
            throw new IllegalArgumentException("Công việc không thể phụ thuộc vào chính nó");
        }

        WbsItem successor = wbsItemRepository.findById(successorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công việc kế nhiệm (successor)"));

        WbsItem predecessor = wbsItemRepository.findById(predId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công việc tiên quyết (predecessor)"));

        UUID projectId = successor.getProjectId();

        if (repository.existsByPredecessorIdAndSuccessorId(predId, successorId)) {
            throw new IllegalArgumentException("Mối quan hệ phụ thuộc này đã tồn tại");
        }

        if (hasPath(projectId, successorId, predId)) {
            throw new IllegalArgumentException("Phát hiện chu trình phụ thuộc vòng tròn (Circular dependency)");
        }

        DependencyType type = request.dependencyType() != null ? request.dependencyType() : DependencyType.FS;
        int lag = request.lagDays() != null ? request.lagDays() : 0;

        TaskDependency dependency = new TaskDependency(projectId, predId, successorId, type, lag);
        return TaskDependencyResponse.from(repository.save(dependency));
    }

    @Transactional
    public void deleteDependency(UUID dependencyId) {
        repository.deleteById(dependencyId);
    }

    private boolean hasPath(UUID projectId, UUID start, UUID target) {
        List<TaskDependency> existing = repository.findByProjectId(projectId);
        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (TaskDependency dep : existing) {
            adj.computeIfAbsent(dep.getPredecessorId(), k -> new ArrayList<>()).add(dep.getSuccessorId());
        }

        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            UUID curr = queue.poll();
            if (curr.equals(target)) {
                return true;
            }
            for (UUID next : adj.getOrDefault(curr, Collections.emptyList())) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return false;
    }
}