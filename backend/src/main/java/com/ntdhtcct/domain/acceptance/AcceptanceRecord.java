package com.ntdhtcct.domain.acceptance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acceptance_records")
public class AcceptanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AcceptanceRecordStatus status = AcceptanceRecordStatus.PENDING_APPROVAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AcceptanceRecord() {
    }

    public AcceptanceRecord(UUID projectId, String title, LocalDate inspectionDate, String description) {
        this.projectId = projectId;
        this.title = title;
        this.inspectionDate = inspectionDate;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AcceptanceRecordStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptanceRecordStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
