package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.util.List;

/**
 * Kết quả của Topological Sort (Kahn's Algorithm) trên một {@link TaskDependencyGraph}.
 *
 * <p>Nếu {@code isComplete()} trả về {@code false}, graph chứa ít nhất một cycle
 * hoặc có node bị kẹt không thể xử lý. Danh sách {@code sortedItems()} vẫn chứa
 * các node đã được sắp xếp thành công theo thứ tự topo trước khi phát hiện sự
 * không đầy đủ. Chi tiết về cycle được xử lý bởi T-17.
 */
public record TopologicalSortResult(
        List<WbsItem> sortedItems,
        boolean isComplete
) {
    public TopologicalSortResult {
        sortedItems = List.copyOf(sortedItems);
    }
}
