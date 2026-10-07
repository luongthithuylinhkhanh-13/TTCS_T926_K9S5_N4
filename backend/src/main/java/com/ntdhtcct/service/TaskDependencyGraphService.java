package com.ntdhtcct.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.CycleDetectionResult;
import com.ntdhtcct.domain.CycleDetector;
import com.ntdhtcct.domain.TaskDependencyGraph;
import com.ntdhtcct.domain.dependency.TaskDependencyRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;

@Service
public class TaskDependencyGraphService {

    private final WbsItemRepository wbsItemRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    public TaskDependencyGraphService(
            WbsItemRepository wbsItemRepository,
            TaskDependencyRepository taskDependencyRepository
    ) {
        this.wbsItemRepository = wbsItemRepository;
        this.taskDependencyRepository = taskDependencyRepository;
    }

    @Transactional(readOnly = true)
    public TaskDependencyGraph getProjectGraph(Long projectId) {

        List<WbsItem> wbsItems =
                wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        if (wbsItems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy công trình với ID: " + projectId
            );
        }

        Set<UUID> taskIds = wbsItems.stream()
                .map(WbsItem::getId)
                .collect(Collectors.toSet());

        List<com.ntdhtcct.domain.dependency.TaskDependency> persistedDependencies =
                taskDependencyRepository
                        .findByPredecessorIdInAndSuccessorIdIn(
                                taskIds,
                                taskIds
                        );

        List<com.ntdhtcct.domain.TaskDependency> dependencies =
                persistedDependencies.stream()
                        .map(dependency ->
                                new com.ntdhtcct.domain.TaskDependency(
                                        dependency.getPredecessorId(),
                                        dependency.getSuccessorId(),
                                        com.ntdhtcct.domain.DependencyType.valueOf(
                                                dependency.getDependencyType().name()
                                        ),
                                        dependency.getLagDays()
                                )
                        )
                        .toList();

        return new TaskDependencyGraph(
                wbsItems,
                dependencies
        );
    }

    @Transactional(readOnly = true)
    public CycleDetectionResult detectCycles(Long projectId) {
        TaskDependencyGraph graph =
                getProjectGraph(projectId);

        return CycleDetector.detect(graph);
    }
}