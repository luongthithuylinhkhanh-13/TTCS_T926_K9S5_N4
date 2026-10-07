package com.ntdhtcct.domain.wbs;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_assignment_history")
public class TeamAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "wbs_id", nullable = false)
    private UUID wbsId;

    @Column(name = "old_team_id")
    private UUID oldTeamId;

    @Column(name = "new_team_id")
    private UUID newTeamId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "reason", length = 500)
    private String reason;

    public TeamAssignmentHistory() {
    }

    public TeamAssignmentHistory(
            Long projectId,
            UUID wbsId,
            UUID oldTeamId,
            UUID newTeamId,
            UUID changedBy,
            String reason
    ) {
        this.projectId = projectId;
        this.wbsId = wbsId;
        this.oldTeamId = oldTeamId;
        this.newTeamId = newTeamId;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public UUID getWbsId() {
        return wbsId;
    }

    public void setWbsId(UUID wbsId) {
        this.wbsId = wbsId;
    }

    public UUID getOldTeamId() {
        return oldTeamId;
    }

    public void setOldTeamId(UUID oldTeamId) {
        this.oldTeamId = oldTeamId;
    }

    public UUID getNewTeamId() {
        return newTeamId;
    }

    public void setNewTeamId(UUID newTeamId) {
        this.newTeamId = newTeamId;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}