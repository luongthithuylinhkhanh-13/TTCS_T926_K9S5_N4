package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopologicalSorterTest {

    // ------------------------------------------------------------------
    // Helper: tạo WbsItem mock với ID và wbsCode (hoặc null wbsCode)
    // ------------------------------------------------------------------

    private WbsItem wbsItem(UUID id, String wbsCode) {
        WbsItem item = mock(WbsItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getWbsCode()).thenReturn(wbsCode);
        return item;
    }

    private WbsItem wbsItemNoCode(UUID id) {
        return wbsItem(id, null);
    }

    // ------------------------------------------------------------------
    // Test 1: null graph
    // ------------------------------------------------------------------

    @Test
    void shouldThrowOnNullGraph() {
        assertThrows(NullPointerException.class, () -> TopologicalSorter.sort(null));
    }

    // ------------------------------------------------------------------
    // Test 2: empty graph
    // ------------------------------------------------------------------

    @Test
    void shouldReturnCompleteEmptyResultForEmptyGraph() {
        TaskDependencyGraph graph = new TaskDependencyGraph(List.of(), List.of());
        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        assertTrue(result.sortedItems().isEmpty());
    }

    // ------------------------------------------------------------------
    // Test 3: isolated nodes (no edges)
    // ------------------------------------------------------------------

    @Test
    void shouldSortIsolatedNodesAllPresent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b), wbsItemNoCode(c)),
                List.of()
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        assertEquals(3, result.sortedItems().size());
        // All three nodes must appear
        List<UUID> sortedIds = result.sortedItems().stream()
                .map(WbsItem::getId)
                .toList();
        assertTrue(sortedIds.containsAll(List.of(a, b, c)));
    }

    // ------------------------------------------------------------------
    // Test 4: linear chain A→B→C
    // ------------------------------------------------------------------

    @Test
    void shouldSortLinearChainInCorrectOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b), wbsItemNoCode(c)),
                List.of(
                        dep(a, b),
                        dep(b, c)
                )
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        List<UUID> sortedIds = result.sortedItems().stream()
                .map(WbsItem::getId)
                .toList();
        assertEquals(3, sortedIds.size());
        assertTrue(sortedIds.indexOf(a) < sortedIds.indexOf(b));
        assertTrue(sortedIds.indexOf(b) < sortedIds.indexOf(c));
    }

    // ------------------------------------------------------------------
    // Test 5: diamond graph A→B, A→C, B→D, C→D
    // ------------------------------------------------------------------

    @Test
    void shouldSortDiamondGraphCorrectly() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b), wbsItemNoCode(c), wbsItemNoCode(d)),
                List.of(
                        dep(a, b),
                        dep(a, c),
                        dep(b, d),
                        dep(c, d)
                )
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        List<UUID> sortedIds = result.sortedItems().stream()
                .map(WbsItem::getId)
                .toList();
        assertEquals(4, sortedIds.size());
        // A must be first, D must be last
        assertEquals(a, sortedIds.get(0));
        assertEquals(d, sortedIds.get(sortedIds.size() - 1));
        // B and C must appear before D
        assertTrue(sortedIds.indexOf(b) < sortedIds.indexOf(d));
        assertTrue(sortedIds.indexOf(c) < sortedIds.indexOf(d));
    }

    // ------------------------------------------------------------------
    // Test 6: wbsCode tiebreaking
    // ------------------------------------------------------------------

    @Test
    void shouldRespectWbsCodeTiebreakingWhenBothHaveInDegreeZero() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        // first has wbsCode "1.1", second has "2.1" — no edges between them
        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItem(first, "1.1"), wbsItem(second, "2.1")),
                List.of()
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        List<UUID> sortedIds = result.sortedItems().stream()
                .map(WbsItem::getId)
                .toList();
        assertEquals(first, sortedIds.get(0),
                "\"1.1\" should come before \"2.1\" when both have inDegree=0");
        assertEquals(second, sortedIds.get(1));
    }

    @Test
    void shouldFallbackToUuidStringWhenWbsCodeIsNull() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        // Both null wbsCode — fallback to UUID string, still deterministic
        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b)),
                List.of()
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        assertEquals(2, result.sortedItems().size());
        // Output is deterministic: UUID string comparison
        List<UUID> ids = result.sortedItems().stream().map(WbsItem::getId).toList();
        UUID expectedFirst = a.toString().compareTo(b.toString()) < 0 ? a : b;
        assertEquals(expectedFirst, ids.get(0));
    }

    // ------------------------------------------------------------------
    // Test 7: K-01 sample — 10 tasks
    //   Task1→Task2→Task3→Task4→Task5 (chain A)
    //   Task6→Task7→Task8→Task9→Task10 (chain B)
    //   Task3→Task7 (cross-link: A3 must precede B7)
    // ------------------------------------------------------------------

    @Test
    void shouldSortK01SampleTenTasksRespectingCrossLink() {
        UUID t1 = UUID.randomUUID(); UUID t2 = UUID.randomUUID();
        UUID t3 = UUID.randomUUID(); UUID t4 = UUID.randomUUID();
        UUID t5 = UUID.randomUUID(); UUID t6 = UUID.randomUUID();
        UUID t7 = UUID.randomUUID(); UUID t8 = UUID.randomUUID();
        UUID t9 = UUID.randomUUID(); UUID t10 = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(
                        wbsItem(t1, "1"), wbsItem(t2, "2"), wbsItem(t3, "3"),
                        wbsItem(t4, "4"), wbsItem(t5, "5"), wbsItem(t6, "6"),
                        wbsItem(t7, "7"), wbsItem(t8, "8"), wbsItem(t9, "9"),
                        wbsItem(t10, "10")
                ),
                List.of(
                        dep(t1, t2), dep(t2, t3), dep(t3, t4), dep(t4, t5),
                        dep(t6, t7), dep(t7, t8), dep(t8, t9), dep(t9, t10),
                        dep(t3, t7)  // cross-link
                )
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertTrue(result.isComplete());
        assertEquals(10, result.sortedItems().size());

        List<UUID> ids = result.sortedItems().stream().map(WbsItem::getId).toList();

        // Chain A order
        assertTrue(ids.indexOf(t1) < ids.indexOf(t2));
        assertTrue(ids.indexOf(t2) < ids.indexOf(t3));
        assertTrue(ids.indexOf(t3) < ids.indexOf(t4));
        assertTrue(ids.indexOf(t4) < ids.indexOf(t5));

        // Chain B order
        assertTrue(ids.indexOf(t6) < ids.indexOf(t7));
        assertTrue(ids.indexOf(t7) < ids.indexOf(t8));
        assertTrue(ids.indexOf(t8) < ids.indexOf(t9));
        assertTrue(ids.indexOf(t9) < ids.indexOf(t10));

        // Cross-link: T3 must appear before T7
        assertTrue(ids.indexOf(t3) < ids.indexOf(t7));
    }

    // ------------------------------------------------------------------
    // Test 8: large graph ~500 tasks — linear chain
    // ------------------------------------------------------------------

    @Test
    void shouldSortLargeLinearGraphOf500Tasks() {
        int size = 500;
        List<UUID> ids = IntStream.range(0, size)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        List<WbsItem> items = IntStream.range(0, size)
                .mapToObj(i -> wbsItemNoCode(ids.get(i)))
                .toList();

        List<TaskDependency> edges = IntStream.range(0, size - 1)
                .mapToObj(i -> dep(ids.get(i), ids.get(i + 1)))
                .toList();

        TaskDependencyGraph graph = new TaskDependencyGraph(items, edges);

        long start = System.currentTimeMillis();
        TopologicalSortResult result = TopologicalSorter.sort(graph);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(result.isComplete());
        assertEquals(500, result.sortedItems().size());

        // Verify predecessor always before successor in output
        List<UUID> sortedIds = result.sortedItems().stream()
                .map(WbsItem::getId)
                .toList();
        for (int i = 0; i < size - 1; i++) {
            assertTrue(
                    sortedIds.indexOf(ids.get(i)) < sortedIds.indexOf(ids.get(i + 1)),
                    "Task " + i + " must appear before task " + (i + 1)
            );
        }

        assertTrue(elapsed < 1000,
                "Sort of 500 tasks should complete in under 1s, took: " + elapsed + "ms");
    }

    // ------------------------------------------------------------------
    // Test 9: incomplete sort when cycle exists (A→B→A)
    // ------------------------------------------------------------------

    @Test
    void shouldReturnIncompleteResultWhenCycleExists() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b)),
                List.of(dep(a, b), dep(b, a))
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertFalse(result.isComplete(),
                "Graph with cycle A→B→A cannot be fully topologically sorted");
        assertTrue(result.sortedItems().size() < 2,
                "Cycle nodes should not appear in sorted output");
        assertNotNull(result.sortedItems());
    }

    // ------------------------------------------------------------------
    // Test 10: 3-node cycle (A→B→C→A) with one isolated node D
    // ------------------------------------------------------------------

    @Test
    void shouldSortIsolatedNodeButNotCycleNodes() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID(); // isolated

        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a), wbsItemNoCode(b), wbsItemNoCode(c), wbsItemNoCode(d)),
                List.of(dep(a, b), dep(b, c), dep(c, a)) // cycle on A,B,C; D is isolated
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertFalse(result.isComplete());
        // D has inDegree=0 so it gets sorted; A,B,C are stuck in cycle
        assertEquals(1, result.sortedItems().size());
        assertEquals(d, result.sortedItems().get(0).getId());
    }

    // ------------------------------------------------------------------
    // Test 11: result sortedItems() is immutable
    // ------------------------------------------------------------------

    @Test
    void shouldReturnImmutableSortedItemsList() {
        UUID a = UUID.randomUUID();
        TaskDependencyGraph graph = new TaskDependencyGraph(
                List.of(wbsItemNoCode(a)),
                List.of()
        );

        TopologicalSortResult result = TopologicalSorter.sort(graph);

        assertThrows(UnsupportedOperationException.class,
                () -> ((List<WbsItem>) result.sortedItems()).add(null),
                "sortedItems() should be immutable");
    }

    // ------------------------------------------------------------------
    // Utility: create TaskDependency (FS, lag=0)
    // ------------------------------------------------------------------

    private TaskDependency dep(UUID predecessorId, UUID successorId) {
        return new TaskDependency(predecessorId, successorId, DependencyType.FS, 0);
    }
}
