package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskDependencyGraphTest {

    @Test
    void shouldRepresentPredecessorToSuccessorAndKeepDependencyMetadata() {
        UUID predecessorId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        TaskDependency dependency = new TaskDependency(
                predecessorId,
                successorId,
                DependencyType.SS,
                -2
        );

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(predecessorId), wbsItem(successorId)),
                List.of(dependency)
        );

        assertEquals(List.of(dependency), graph.getOutgoingDependencies(predecessorId));
        assertEquals(List.of(dependency), graph.getIncomingDependencies(successorId));
        assertEquals(DependencyType.SS, graph.getOutgoingDependencies(predecessorId).get(0).getDependencyType());
        assertEquals(-2, graph.getOutgoingDependencies(predecessorId).get(0).getLagDays());
    }

    @Test
    void shouldRepresentMultipleIncomingAndOutgoingDependencies() {
        UUID taskA = UUID.randomUUID();
        UUID taskB = UUID.randomUUID();
        UUID taskC = UUID.randomUUID();
        UUID taskD = UUID.randomUUID();

        TaskDependency edgeAtoB = new TaskDependency(taskA, taskB, DependencyType.FS, 0);
        TaskDependency edgeAtoC = new TaskDependency(taskA, taskC, DependencyType.FF, 1);
        TaskDependency edgeDtoB = new TaskDependency(taskD, taskB, DependencyType.SF, 3);

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(taskA), wbsItem(taskB), wbsItem(taskC), wbsItem(taskD)),
                List.of(edgeAtoB, edgeAtoC, edgeDtoB)
        );

        assertEquals(List.of(edgeAtoB, edgeAtoC), graph.getOutgoingDependencies(taskA));
        assertEquals(List.of(edgeAtoB, edgeDtoB), graph.getIncomingDependencies(taskB));
    }

    @Test
    void shouldKeepIsolatedWbsItemsAsNodesWithEmptyAdjacency() {
        UUID isolatedId = UUID.randomUUID();
        WbsItem isolatedItem = wbsItem(isolatedId);

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(isolatedItem),
                List.of()
        );

        assertEquals(isolatedItem, graph.findNode(isolatedId).orElseThrow());
        assertTrue(graph.getOutgoingDependencies(isolatedId).isEmpty());
        assertTrue(graph.getIncomingDependencies(isolatedId).isEmpty());
    }

    @Test
    void shouldRejectDependencyWithEndpointOutsideGraph() {
        UUID knownId = UUID.randomUUID();
        TaskDependency invalidDependency = new TaskDependency(
                knownId,
                UUID.randomUUID(),
                DependencyType.FS,
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new TaskDependencyGraph(
                        List.of(wbsItem(knownId)),
                        List.of(invalidDependency)
                )
        );

        assertEquals(
                "Dependency endpoints must belong to the dependency graph",
                exception.getMessage()
        );
    }

    @Test
    void shouldComputeInDegreeAndOutDegreeCorrectly() {
        UUID predecessorId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        TaskDependency dependency = new TaskDependency(
                predecessorId,
                successorId,
                DependencyType.FS,
                0
        );

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(predecessorId), wbsItem(successorId)),
                List.of(dependency)
        );

        assertEquals(0, graph.getInDegree(predecessorId));
        assertEquals(1, graph.getOutDegree(predecessorId));
        assertEquals(1, graph.getInDegree(successorId));
        assertEquals(0, graph.getOutDegree(successorId));
    }

    @Test
    void shouldComputeDegreesWithMultipleIncomingAndOutgoingEdges() {
        UUID taskA = UUID.randomUUID();
        UUID taskB = UUID.randomUUID();
        UUID taskC = UUID.randomUUID();
        UUID taskD = UUID.randomUUID();

        TaskDependency edgeAtoB = new TaskDependency(taskA, taskB, DependencyType.FS, 0);
        TaskDependency edgeAtoC = new TaskDependency(taskA, taskC, DependencyType.FF, 1);
        TaskDependency edgeDtoB = new TaskDependency(taskD, taskB, DependencyType.SF, 3);

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(taskA), wbsItem(taskB), wbsItem(taskC), wbsItem(taskD)),
                List.of(edgeAtoB, edgeAtoC, edgeDtoB)
        );

        assertEquals(0, graph.getInDegree(taskA));
        assertEquals(2, graph.getOutDegree(taskA));

        assertEquals(2, graph.getInDegree(taskB));
        assertEquals(0, graph.getOutDegree(taskB));

        assertEquals(1, graph.getInDegree(taskC));
        assertEquals(0, graph.getOutDegree(taskC));

        assertEquals(0, graph.getInDegree(taskD));
        assertEquals(1, graph.getOutDegree(taskD));

        assertEquals(
                java.util.Map.of(
                        taskA, 0,
                        taskB, 2,
                        taskC, 1,
                        taskD, 0
                ),
                graph.getInDegrees()
        );
    }

    @Test
    void shouldReturnZeroDegreesForIsolatedNode() {
        UUID isolatedId = UUID.randomUUID();
        WbsItem isolatedItem = wbsItem(isolatedId);

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(isolatedItem),
                List.of()
        );

        assertEquals(0, graph.getInDegree(isolatedId));
        assertEquals(0, graph.getOutDegree(isolatedId));
        assertEquals(java.util.Map.of(isolatedId, 0), graph.getInDegrees());
    }

    @Test
    void shouldThrowWhenQueryingUnknownNode() {
        UUID knownId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(knownId)),
                List.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.getInDegree(unknownId)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.getOutDegree(unknownId)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.getIncomingDependencies(unknownId)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.getOutgoingDependencies(unknownId)
        );
        assertThrows(
                NullPointerException.class,
                () -> graph.getInDegree(null)
        );
        assertTrue(graph.containsNode(knownId));
        org.junit.jupiter.api.Assertions.assertFalse(graph.containsNode(unknownId));
        org.junit.jupiter.api.Assertions.assertFalse(graph.containsNode(null));
        assertTrue(graph.findNode(unknownId).isEmpty());
        assertTrue(graph.findNode(null).isEmpty());
    }

    private WbsItem wbsItem(UUID id) {
        WbsItem item = mock(WbsItem.class);
        when(item.getId()).thenReturn(id);
        return item;
    }
}
