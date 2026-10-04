package com.ntdhtcct.domain.wbs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "project_schedule_baseline_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_baseline_project_item",
                columnNames = {"project_id", "item_id"}
        )
)
public class ProjectScheduleBaselineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "wbs_code", nullable = false, length = 50, updatable = false)
    private String wbsCode;

    @Column(name = "name", nullable = false, length = 255, updatable = false)
    private String name;

    @Column(name = "start_date", updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", updatable = false)
    private LocalDate endDate;

    @Column(name = "duration", updatable = false)
    private Integer duration;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private OffsetDateTime capturedAt;

    protected ProjectScheduleBaselineItem() {
    }

    public ProjectScheduleBaselineItem(
            UUID projectId,
            UUID itemId,
            String wbsCode,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Integer duration,
            OffsetDateTime capturedAt
    ) {
        this.projectId = projectId;
        this.itemId = itemId;
        this.wbsCode = wbsCode;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.capturedAt = capturedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getWbsCode() {
        return wbsCode;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }
}
