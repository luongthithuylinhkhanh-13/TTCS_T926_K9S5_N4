package com.ntdhtcct.domain.acceptance.dto;

import com.ntdhtcct.domain.acceptance.AcceptanceRecord;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AcceptanceRecordResponse(
        UUID id,
        UUID projectId,
        String title,
        LocalDate inspectionDate,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AcceptanceRecordResponse from(AcceptanceRecord record) {
        return new AcceptanceRecordResponse(
                record.getId(),
                record.getProjectId(),
                record.getTitle(),
                record.getInspectionDate(),
                record.getDescription(),
                record.getStatus().name(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
