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
        UUID wbsItemId = null;
        String wbsItemName = null;
        String wbsCode = null;
        try {
            if (cost.getWbsItem() != null) {
                wbsItemId = cost.getWbsItem().getId();
                wbsItemName = cost.getWbsItem().getName();
                wbsCode = cost.getWbsItem().getWbsCode();
            }
        } catch (Exception ignored) {
        }

        UUID taskId = null;
        String taskName = null;
        try {
            if (cost.getTask() != null) {
                taskId = cost.getTask().getId();
                taskName = cost.getTask().getName();
            }
        } catch (Exception ignored) {
        }

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
