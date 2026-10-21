package com.ntdhtcct.domain.acceptance.dto;

import com.ntdhtcct.domain.acceptance.AcceptanceRecordAuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AcceptanceRecordAuditLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String action,
        String comment,
        OffsetDateTime performedAt
) {
    public static AcceptanceRecordAuditLogResponse from(AcceptanceRecordAuditLog log) {
        return new AcceptanceRecordAuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorName(),
                log.getAction().name(),
                log.getComment(),
                log.getPerformedAt()
        );
    }
}
