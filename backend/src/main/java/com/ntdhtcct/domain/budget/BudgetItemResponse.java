package com.ntdhtcct.domain.budget;

import java.util.List;
import java.util.UUID;

public record BudgetItemResponse(
        UUID id,
        UUID projectId,
        UUID parentId,
        String wbsCode,
        String name,
        String type,
        String status,
        boolean isLeaf,
        Double quantity,
        String unit,
        Double unitPrice,
        Double totalPrice,
        Double directTotalPrice,
        Double rollupTotalPrice,
        int childCount,
        List<BudgetItemResponse> children
) {
}
