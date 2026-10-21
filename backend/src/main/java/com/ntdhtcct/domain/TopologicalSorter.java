package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Sắp xếp topo một {@link TaskDependencyGraph} bằng Kahn's Algorithm.
 *
 * <ul>
 *   <li>Iterative (queue-based) — không dùng đệ quy.</li>
 *   <li>Pure domain code — không phụ thuộc Spring, JPA, HTTP, hay repository.</li>
 *   <li>Khi graph không hoàn toàn DAG (có cycle), {@link TopologicalSortResult#isComplete()} là
 *       {@code false}; danh sách chứa các node đã xử lý được.
 *       Chi tiết cycle detection được xử lý bởi T-17.</li>
 *   <li>Tiebreak giữa các node có inDegree = 0: {@code wbsCode} lexicographic nếu non-null,
 *       fallback về {@code UUID.toString()}. Không có T-16 requirement nào thêm lên
 *       {@code wbsCode}.</li>
 * </ul>
 */
public final class TopologicalSorter {

    private TopologicalSorter() {
    }

    /**
     * Thực hiện topological sort trên toàn bộ node của {@code graph}.
     *
     * @param graph TaskDependencyGraph đã build từ T-15 — không được null
     * @return {@link TopologicalSortResult} chứa danh sách đã sắp xếp và cờ {@code isComplete}
     */
    public static TopologicalSortResult sort(TaskDependencyGraph graph) {
        Objects.requireNonNull(graph, "Graph must not be null");

        Map<UUID, WbsItem> nodes = graph.getNodes();

        if (nodes.isEmpty()) {
            return new TopologicalSortResult(List.of(), true);
        }

        // 1. Build mutable inDegree map từ immutable getInDegrees()
        Map<UUID, Integer> inDegree = new HashMap<>(graph.getInDegrees());

        // 2. Comparator: wbsCode lexicographic nếu non-null, fallback UUID string.
        //    Chỉ dùng wbsCode khi có sẵn — không thêm requirement mới lên T-16.
        Comparator<UUID> byWbsCodeThenUuid = Comparator.comparing(
                id -> {
                    WbsItem item = nodes.get(id);
                    String code = item.getWbsCode();
                    return (code != null && !code.isEmpty()) ? code : id.toString();
                }
        );

        // 3. Seed PriorityQueue với tất cả node có inDegree = 0
        PriorityQueue<UUID> queue = new PriorityQueue<>(byWbsCodeThenUuid);
        inDegree.forEach((id, degree) -> {
            if (degree == 0) {
                queue.add(id);
            }
        });

        // 4. Kahn's BFS
        List<WbsItem> sorted = new ArrayList<>(nodes.size());

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            sorted.add(nodes.get(current));

            for (TaskDependency outgoing : graph.getOutgoingDependencies(current)) {
                UUID successorId = outgoing.getSuccessorId();
                int newDegree = inDegree.merge(successorId, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(successorId);
                }
            }
        }

        // 5. Incomplete = có cycle hoặc node bị kẹt
        boolean isComplete = sorted.size() == nodes.size();

        return new TopologicalSortResult(sorted, isComplete);
    }
}
