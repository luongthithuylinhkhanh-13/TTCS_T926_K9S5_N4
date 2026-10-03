package com.ntdhtcct.service;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.CycleDetectionResult;
import com.ntdhtcct.domain.CycleDetector;
import com.ntdhtcct.domain.TaskDependencyGraph;
import com.ntdhtcct.domain.dependency.TaskDependency;
import com.ntdhtcct.domain.dependency.TaskDependencyRepository;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskDependencyGraphService {

    private final ProjectRepository projectRepository;
    private final WbsItemRepository wbsItemRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    public TaskDependencyGraphService(
            ProjectRepository projectRepository,
            WbsItemRepository wbsItemRepository,
            TaskDependencyRepository taskDependencyRepository
    ) {
        this.projectRepository = projectRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.taskDependencyRepository = taskDependencyRepository;
    }

    @Transactional(readOnly = true)
    public TaskDependencyGraph getProjectGraph(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy công trình với ID: " + projectId
            );
        }

        List<WbsItem> wbsItems =
                wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        if (wbsItems.isEmpty()) {
            return new TaskDependencyGraph(wbsItems, List.of());
        }

        Set<UUID> taskIds = wbsItems.stream()
                .map(WbsItem::getId)
                .collect(Collectors.toSet());

        List<TaskDependency> dependencies =
                taskDependencyRepository
                        .findByPredecessorIdInAndSuccessorIdIn(taskIds, taskIds);

        return new TaskDependencyGraph(wbsItems, dependencies);
    }

    @Transactional(readOnly = true)
    public CycleDetectionResult detectCycles(UUID projectId) {
        TaskDependencyGraph graph = getProjectGraph(projectId);
        return CycleDetector.detect(graph);
    }
}
