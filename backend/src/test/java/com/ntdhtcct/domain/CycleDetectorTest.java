package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CycleDetectorTest {

    private WbsItem wbsItem(UUID id) {
        WbsItem item = mock(WbsItem.class);
        when(item.getId()).thenReturn(id);
        return item;
    }

    private TaskDependency dep(UUID predecessorId, UUID successorId) {
        return new TaskDependency(predecessorId, successorId, DependencyType.FS, 0);
    }

    private Set<UUID> toIdSet(CycleDetectionResult result) {
        return result.cycleNodes().stream()
                .map(WbsItem::getId)
                .collect(Collectors.toSet());
    }

    @Test
    void shouldThrowOnNullGraph() {
        assertThrows(NullPointerException.class, () -> CycleDetector.detect(null));
    }

    @Test
    void shouldReturnNoCycleForEmptyGraph() {
        TaskDependencyGraph graph = new TaskDependencyGraph(List.of(), List.of());
        CycleDetectionResult result = CycleDetector.detect(graph);

        assertFalse(result.hasCycle());
        assertTrue(result.cycleNodes().isEmpty());
    }

    @Test
    void shouldReturnNoCycleForLinearChain() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b), wbsItem(c)),
                List.of(dep(a, b), dep(b, c))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertFalse(result.hasCycle());
        assertTrue(result.cycleNodes().isEmpty());
    }

    @Test
    void shouldReturnNoCycleForDiamond() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b), wbsItem(c), wbsItem(d)),
                List.of(dep(a, b), dep(a, c), dep(b, d), dep(c, d))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertFalse(result.hasCycle());
        assertTrue(result.cycleNodes().isEmpty());
    }

    @Test
    void shouldDetectSelfLoop() {
        UUID a = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a)),
                List.of(dep(a, a))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        assertEquals(Set.of(a), toIdSet(result));
    }

    @Test
    void shouldDetectTwoNodeCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b)),
                List.of(dep(a, b), dep(b, a))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        assertEquals(Set.of(a, b), toIdSet(result));
    }

    @Test
    void shouldDetectFourNodeCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b), wbsItem(c), wbsItem(d)),
                List.of(dep(a, b), dep(b, c), dep(c, d), dep(d, a))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        assertEquals(Set.of(a, b, c, d), toIdSet(result));
    }

    @Test
    void shouldExcludeDownstreamNodeFromCycle() {
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID(); // downstream of cycle B<->C, NOT in cycle

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(b), wbsItem(c), wbsItem(d)),
                List.of(dep(b, c), dep(c, b), dep(b, d))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        Set<UUID> cycleIds = toIdSet(result);
        assertEquals(Set.of(b, c), cycleIds);
        assertFalse(cycleIds.contains(d));
    }

    @Test
    void shouldExcludeUpstreamNodeFromCycle() {
        UUID a = UUID.randomUUID(); // upstream feeding into cycle B<->C, NOT in cycle
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b), wbsItem(c)),
                List.of(dep(a, b), dep(b, c), dep(c, b))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        Set<UUID> cycleIds = toIdSet(result);
        assertEquals(Set.of(b, c), cycleIds);
        assertFalse(cycleIds.contains(a));
    }

    @Test
    void shouldDetectMultipleDisjointCycles() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        UUID e = UUID.randomUUID();
        UUID f = UUID.randomUUID(); // isolated

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a), wbsItem(b), wbsItem(c), wbsItem(d), wbsItem(e), wbsItem(f)),
                List.of(
                        dep(a, b), dep(b, a),
                        dep(c, d), dep(d, e), dep(e, c)
                )
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertTrue(result.hasCycle());
        Set<UUID> cycleIds = toIdSet(result);
        assertEquals(Set.of(a, b, c, d, e), cycleIds);
        assertFalse(cycleIds.contains(f));
    }

    @Test
    void shouldReturnImmutableCycleNodesList() {
        UUID a = UUID.randomUUID();
        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(a)),
                List.of(dep(a, a))
        );

        CycleDetectionResult result = CycleDetector.detect(graph);

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.cycleNodes().add(wbsItem(UUID.randomUUID()))
        );
    }
}
