package com.ntdhtcct.domain.cost.dto;

import com.ntdhtcct.domain.cost.ActualCost;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActualCostResponse(
        UUID id,
        UUID projectId,
        UUID wbsItemId,
        String wbsItemName,
        String wbsCode,
        UUID taskId,
        String taskName,
        String name,
        Double amount,
        String costType,
        LocalDate incurredDate,
        String note,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ActualCostResponse from(ActualCost cost) {
        UUID wbsItemId = cost.getWbsItem() != null ? cost.getWbsItem().getId() : null;
        String wbsItemName = cost.getWbsItem() != null ? cost.getWbsItem().getName() : null;
        String wbsCode = cost.getWbsItem() != null ? cost.getWbsItem().getWbsCode() : null;
        UUID taskId = cost.getTask() != null ? cost.getTask().getId() : null;
        String taskName = cost.getTask() != null ? cost.getTask().getName() : null;

        return new ActualCostResponse(
                cost.getId(),
                cost.getProjectId(),
                wbsItemId,
                wbsItemName,
                wbsCode,
                taskId,
                taskName,
                cost.getName(),
                cost.getAmount(),
                cost.getCostType(),
                cost.getIncurredDate(),
                cost.getNote(),
                cost.getCreatedBy(),
                cost.getCreatedAt(),
                cost.getUpdatedAt()
        );
    }
}
