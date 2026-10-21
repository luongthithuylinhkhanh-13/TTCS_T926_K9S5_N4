package com.ntdhtcct.domain.cost.dto;

import java.util.UUID;

public record ActualCostSummaryResponse(
        UUID projectId,
        UUID wbsItemId,
        Double totalActualCost,
        Long totalRecords
) {
}
