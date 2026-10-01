package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Phát hiện chu trình trong {@link TaskDependencyGraph} bằng Kosaraju's Algorithm (iterative).
 *
 * <ul>
 *   <li>Không dùng đệ quy — explicit Deque stack.</li>
 *   <li>Pure domain — không phụ thuộc Spring, JPA, HTTP, repository.</li>
 *   <li>Độ phức tạp: O(V+E).</li>
 *   <li>Self-loop (A→A) được phát hiện bằng pre-scan riêng vì standard Kosaraju's
 *       không flag SCC size 1 là cycle.</li>
 * </ul>
 */
public final class CycleDetector {

    private CycleDetector() {
    }

    /**
     * Phát hiện chu trình trong {@code graph}.
     *
     * @param graph TaskDependencyGraph đã build từ T-15 — không được null
     * @return {@link CycleDetectionResult} chứa các WbsItem thuộc cycle
     */
    public static CycleDetectionResult detect(TaskDependencyGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null");

        Map<UUID, WbsItem> nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            return new CycleDetectionResult(List.of());
        }

        // Step 1: pre-scan self-loops (A -> A)
        Set<UUID> cycleNodeIds = new HashSet<>();
        for (UUID nodeId : nodes.keySet()) {
            for (TaskDependency dep : graph.getOutgoingDependencies(nodeId)) {
                if (dep.getSuccessorId().equals(nodeId)) {
                    cycleNodeIds.add(nodeId);
                }
            }
        }

        // Step 2: Kosaraju's Pass 1 — forward DFS, finish order
        Deque<UUID> finishStack = computeFinishOrder(graph, nodes.keySet());

        // Step 3: Kosaraju's Pass 2 — transposed DFS, collect SCCs of size >= 2
        collectMultiNodeCycles(graph, finishStack, cycleNodeIds);

        // Step 4: build result
        List<WbsItem> cycleNodes = new ArrayList<>();
        cycleNodeIds.forEach(id -> cycleNodes.add(nodes.get(id)));
        return new CycleDetectionResult(cycleNodes);
    }

    /**
     * Pass 1: iterative DFS (forward graph), push nodes to finishStack on completion.
     * DFS frame = Map.Entry<UUID, Iterator<TaskDependency>> — simulates recursive frame.
     */
    private static Deque<UUID> computeFinishOrder(
            TaskDependencyGraph graph,
            Set<UUID> allIds
    ) {
        Deque<UUID> finishStack = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();

        for (UUID startId : allIds) {
            if (visited.contains(startId)) {
                continue;
            }

            visited.add(startId);
            Deque<Map.Entry<UUID, Iterator<TaskDependency>>> dfsStack = new ArrayDeque<>();
            dfsStack.push(Map.entry(
                    startId,
                    graph.getOutgoingDependencies(startId).iterator()
            ));

            while (!dfsStack.isEmpty()) {
                Map.Entry<UUID, Iterator<TaskDependency>> frame = dfsStack.peek();
                Iterator<TaskDependency> it = frame.getValue();

                if (it.hasNext()) {
                    UUID neighbor = it.next().getSuccessorId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        dfsStack.push(Map.entry(
                                neighbor,
                                graph.getOutgoingDependencies(neighbor).iterator()
                        ));
                    }
                } else {
                    finishStack.push(dfsStack.pop().getKey());
                }
            }
        }

        return finishStack;
    }

    /**
     * Pass 2: iterative DFS on transposed graph (incoming edges), reverse finish order.
     * Each connected component = one SCC; size >= 2 → all members added to cycleNodeIds.
     */
    private static void collectMultiNodeCycles(
            TaskDependencyGraph graph,
            Deque<UUID> finishStack,
            Set<UUID> cycleNodeIds
    ) {
        Set<UUID> visited = new HashSet<>();

        while (!finishStack.isEmpty()) {
            UUID startId = finishStack.pop();
            if (visited.contains(startId)) {
                continue;
            }

            List<UUID> scc = new ArrayList<>();
            visited.add(startId);
            scc.add(startId);

            Deque<Map.Entry<UUID, Iterator<TaskDependency>>> dfsStack = new ArrayDeque<>();
            dfsStack.push(Map.entry(
                    startId,
                    graph.getIncomingDependencies(startId).iterator()
            ));

            while (!dfsStack.isEmpty()) {
                Map.Entry<UUID, Iterator<TaskDependency>> frame = dfsStack.peek();
                Iterator<TaskDependency> it = frame.getValue();

                if (it.hasNext()) {
                    UUID neighbor = it.next().getPredecessorId();
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        scc.add(neighbor);
                        dfsStack.push(Map.entry(
                                neighbor,
                                graph.getIncomingDependencies(neighbor).iterator()
                        ));
                    }
                } else {
                    dfsStack.pop();
                }
            }

            if (scc.size() >= 2) {
                cycleNodeIds.addAll(scc);
            }
        }
    }
}
