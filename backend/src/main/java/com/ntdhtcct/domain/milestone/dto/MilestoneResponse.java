package com.ntdhtcct.domain.milestone.dto;

import com.ntdhtcct.domain.milestone.Milestone;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID projectId,
        UUID categoryId,
        String categoryName,
        String categoryWbsCode,
        String name,
        LocalDate targetDate,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProjectId(),
                milestone.getCategory() != null ? milestone.getCategory().getId() : null,
                milestone.getCategory() != null ? milestone.getCategory().getName() : null,
                milestone.getCategory() != null ? milestone.getCategory().getWbsCode() : null,
                milestone.getName(),
                milestone.getTargetDate(),
                milestone.getDescription(),
                milestone.getCreatedAt(),
                milestone.getUpdatedAt()
        );
    }
}
