package com.ntdhtcct.domain.budget;

import java.util.UUID;

public record ProjectBudgetSummaryResponse(
        UUID projectId,
        String projectCode,
        String projectName,
        Double totalBudget,
        int totalItems,
        int totalLeafItems,
        int budgetedLeafItems,
        int totalParentItems,
        double completionRate
) {
}
