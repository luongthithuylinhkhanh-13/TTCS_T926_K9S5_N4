package com.ntdhtcct.domain.wbs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_assignment_history")
public class TeamAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "wbs_item_id", nullable = false)
    private UUID wbsItemId;

    @Column(name = "from_team_member_id")
    private UUID fromTeamMemberId;

    @Column(name = "from_team_name")
    private String fromTeamName;

    @Column(name = "to_team_member_id")
    private UUID toTeamMemberId;

    @Column(name = "to_team_name")
    private String toTeamName;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "changed_by_name")
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    protected TeamAssignmentHistory() {
    }

    public TeamAssignmentHistory(
            UUID projectId,
            UUID wbsItemId,
            UUID fromTeamMemberId,
            String fromTeamName,
            UUID toTeamMemberId,
            String toTeamName,
            UUID changedByUserId,
            String changedByName
    ) {
        this.projectId = projectId;
        this.wbsItemId = wbsItemId;
        this.fromTeamMemberId = fromTeamMemberId;
        this.fromTeamName = fromTeamName;
        this.toTeamMemberId = toTeamMemberId;
        this.toTeamName = toTeamName;
        this.changedByUserId = changedByUserId;
        this.changedByName = changedByName;
    }

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWbsItemId() {
        return wbsItemId;
    }

    public UUID getFromTeamMemberId() {
        return fromTeamMemberId;
    }

    public String getFromTeamName() {
        return fromTeamName;
    }

    public UUID getToTeamMemberId() {
        return toTeamMemberId;
    }

    public String getToTeamName() {
        return toTeamName;
    }

    public UUID getChangedByUserId() {
        return changedByUserId;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
}
