package com.ntdhtcct.service;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.CycleDetectionResult;
import com.ntdhtcct.domain.DependencyType;
import com.ntdhtcct.domain.TaskDependency;
import com.ntdhtcct.domain.TaskDependencyGraph;
import com.ntdhtcct.domain.dependency.TaskDependencyRepository;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDependencyGraphServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsItemRepository wbsItemRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @InjectMocks
    private TaskDependencyGraphService graphService;

    @Test
    void shouldReturnEmptyGraphForExistingProjectWithoutWbsItems() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of());

        TaskDependencyGraph graph = graphService.getProjectGraph(projectId);

        assertTrue(graph.getNodes().isEmpty());
        verify(taskDependencyRepository, never())
                .findByPredecessorIdInAndSuccessorIdIn(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldBuildGraphFromOnlyTheRequestedProjectsWbsItemsAndDependencies() {
        UUID projectId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        UUID isolatedId = UUID.randomUUID();
        WbsItem predecessor = wbsItem(predecessorId);
        WbsItem successor = wbsItem(successorId);
        WbsItem isolated = wbsItem(isolatedId);
        com.ntdhtcct.domain.dependency.TaskDependency dependency =
        new com.ntdhtcct.domain.dependency.TaskDependency(
                projectId,
                predecessorId,
                successorId,
                com.ntdhtcct.domain.dependency.DependencyType.FS,
                2
        );
        Set<UUID> expectedTaskIds = Set.of(predecessorId, successorId, isolatedId);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(predecessor, successor, isolated));
        when(taskDependencyRepository.findByPredecessorIdInAndSuccessorIdIn(
                eq(expectedTaskIds),
                eq(expectedTaskIds)
        )).thenReturn(List.of(dependency));

        TaskDependencyGraph graph = graphService.getProjectGraph(projectId);

        assertEquals(expectedTaskIds, graph.getNodes().keySet());
        com.ntdhtcct.domain.TaskDependency graphDependency =
                graph.getOutgoingDependencies(predecessorId).getFirst();
        assertEquals(predecessorId, graphDependency.getPredecessorId());
        assertEquals(successorId, graphDependency.getSuccessorId());
        assertEquals(DependencyType.FS, graphDependency.getDependencyType());
        assertEquals(2, graphDependency.getLagDays());
        assertTrue(graph.getOutgoingDependencies(isolatedId).isEmpty());
        verify(taskDependencyRepository).findByPredecessorIdInAndSuccessorIdIn(
                eq(expectedTaskIds),
                eq(expectedTaskIds)
        );
    }

    @Test
    void shouldRejectUnknownProject() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> graphService.getProjectGraph(projectId)
        );

        verify(wbsItemRepository, never())
                .findByProjectIdOrderByWbsCodeAsc(projectId);
        verify(taskDependencyRepository, never())
                .findByPredecessorIdInAndSuccessorIdIn(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldDetectCyclesViaService() {
        UUID projectId = UUID.randomUUID();
        UUID nodeA = UUID.randomUUID();
        UUID nodeB = UUID.randomUUID();
        WbsItem itemA = wbsItem(nodeA);
        WbsItem itemB = wbsItem(nodeB);
        com.ntdhtcct.domain.dependency.TaskDependency edgeAtoB =
                new com.ntdhtcct.domain.dependency.TaskDependency(
                        projectId, nodeA, nodeB,
                        com.ntdhtcct.domain.dependency.DependencyType.FS, 0
                );
        com.ntdhtcct.domain.dependency.TaskDependency edgeBtoA =
                new com.ntdhtcct.domain.dependency.TaskDependency(
                        projectId, nodeB, nodeA,
                        com.ntdhtcct.domain.dependency.DependencyType.FS, 0
                );
        Set<UUID> taskIds = Set.of(nodeA, nodeB);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(itemA, itemB));
        when(taskDependencyRepository.findByPredecessorIdInAndSuccessorIdIn(
                eq(taskIds), eq(taskIds)))
                .thenReturn(List.of(edgeAtoB, edgeBtoA));

        CycleDetectionResult result = graphService.detectCycles(projectId);

        assertTrue(result.hasCycle());
        Set<UUID> cycleIds = result.cycleNodes().stream()
                .map(WbsItem::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(nodeA, nodeB), cycleIds);
    }

    private WbsItem wbsItem(UUID id) {
        WbsItem item = mock(WbsItem.class);
        when(item.getId()).thenReturn(id);
        return item;
    }
}
