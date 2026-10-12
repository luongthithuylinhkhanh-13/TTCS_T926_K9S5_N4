package com.ntdhtcct.domain.wbs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_reports")
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "wbs_item_id", nullable = false)
    private UUID wbsItemId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "reported_volume", nullable = false)
    private Double reportedVolume;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected DailyReport() {
    }

    public DailyReport(UUID wbsItemId, UUID reporterId, LocalDate reportDate, Double reportedVolume, String notes) {
        this.wbsItemId = wbsItemId;
        this.reporterId = reporterId;
        this.reportDate = reportDate;
        this.reportedVolume = reportedVolume;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWbsItemId() {
        return wbsItemId;
    }

    public void setWbsItemId(UUID wbsItemId) {
        this.wbsItemId = wbsItemId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Double getReportVolume() {
        return reportedVolume;
    }

    public void setReportVolume(Double reportedVolume) {
        this.reportedVolume = reportedVolume;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
