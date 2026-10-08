package com.ntdhtcct.domain.wbs;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "wbs_items")
public class WbsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "wbs_code", nullable = false, length = 50)
    private String wbsCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "assignee_name", length = 255)
    private String assigneeName;

    @Column(name = "assignee_initials", length = 20)
    private String assigneeInitials;

    @Column(name = "assigned_team_member_id")
    private UUID assignedTeamMemberId;

    @Column(name = "assigned_team_name", length = 255)
    private String assignedTeamName;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "progress", nullable = false)
    private int progress = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "es")
    private Integer es;

    @Column(name = "ef")
    private Integer ef;

    @Column(name = "ls")
    private Integer ls;

    @Column(name = "lf")
    private Integer lf;

    @Column(name = "slack")
    private Integer slack;

    @Column(name = "is_critical", nullable = false)
    private boolean critical;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "successor_id")
    )
        @Column(name = "predecessor_id", nullable = false)
    private Set<UUID> predecessorIds = new HashSet<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image", length = 500)
    private String image;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected WbsItem() {
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getWbsCode() {
        return wbsCode;
    }

    public void setWbsCode(String wbsCode) {
        this.wbsCode = wbsCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getAssigneeInitials() {
        return assigneeInitials;
    }

    public void setAssigneeInitials(String assigneeInitials) {
        this.assigneeInitials = assigneeInitials;
    }

    public UUID getAssignedTeamMemberId() {
        return assignedTeamMemberId;
    }

    public void setAssignedTeamMemberId(UUID assignedTeamMemberId) {
        this.assignedTeamMemberId = assignedTeamMemberId;
    }

    public String getAssignedTeamName() {
        return assignedTeamName;
    }

    public void setAssignedTeamName(String assignedTeamName) {
        this.assignedTeamName = assignedTeamName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @JsonProperty("progressPercent")
    public int getProgressPercent() {
        return progress;
    }

    @JsonProperty("progressPercent")
    public void setProgressPercent(int progressPercent) {
        this.progress = progressPercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(LocalDate actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getEs() {
        return es;
    }

    public void setEs(Integer es) {
        this.es = es;
    }

    public Integer getEf() {
        return ef;
    }

    public void setEf(Integer ef) {
        this.ef = ef;
    }

    public Integer getLs() {
        return ls;
    }

    public void setLs(Integer ls) {
        this.ls = ls;
    }

    public Integer getLf() {
        return lf;
    }

    public void setLf(Integer lf) {
        this.lf = lf;
    }

    public Integer getSlack() {
        return slack;
    }

    public void setSlack(Integer slack) {
        this.slack = slack;
    }

    @JsonProperty("isCritical")
    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public Set<UUID> getPredecessorIds() {
        return predecessorIds;
    }

    public void setPredecessorIds(Set<UUID> predecessorIds) {
        this.predecessorIds = predecessorIds == null
                ? new HashSet<>()
                : new HashSet<>(predecessorIds);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}