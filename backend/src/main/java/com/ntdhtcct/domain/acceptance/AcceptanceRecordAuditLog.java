package com.ntdhtcct.domain.acceptance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acceptance_record_audit_logs")
public class AcceptanceRecordAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "acceptance_record_id", nullable = false, updatable = false)
    private UUID acceptanceRecordId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "actor_name", nullable = false, updatable = false, length = 255)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private AcceptanceRecordAction action;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private OffsetDateTime performedAt;

    protected AcceptanceRecordAuditLog() {
    }

    public AcceptanceRecordAuditLog(
            UUID acceptanceRecordId,
            UUID actorId,
            String actorName,
            AcceptanceRecordAction action,
            String comment
    ) {
        this.acceptanceRecordId = acceptanceRecordId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.comment = comment;
    }

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getAcceptanceRecordId() {
        return acceptanceRecordId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public AcceptanceRecordAction getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public OffsetDateTime getPerformedAt() {
        return performedAt;
    }
}
