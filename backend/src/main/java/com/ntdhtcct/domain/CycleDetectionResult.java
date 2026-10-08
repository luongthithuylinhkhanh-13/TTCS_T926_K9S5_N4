package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.util.List;

/**
 * Kết quả phát hiện chu trình (Cycle Detection) trên một {@link TaskDependencyGraph}.
 *
 * <p>{@code cycleNodes()} chứa các WBS item thuộc ít nhất một chu trình.
 * Nodes downstream của chu trình (bị kẹt do predecessor trong cycle)
 * không xuất hiện trong danh sách này.
 *
 * <p>Thứ tự trong {@code cycleNodes()} không được đảm bảo.
 * Caller nên dùng Set equality để kiểm tra membership.
 */
public record CycleDetectionResult(List<WbsItem> cycleNodes) {

    public CycleDetectionResult {
        cycleNodes = List.copyOf(cycleNodes);
    }

    /**
     * @return {@code true} nếu graph có ít nhất một chu trình
     */
    public boolean hasCycle() {
        return !cycleNodes.isEmpty();
    }
}
