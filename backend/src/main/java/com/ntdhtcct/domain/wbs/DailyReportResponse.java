package com.ntdhtcct.domain.wbs;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DailyReportResponse {
    private UUID id;
    private UUID wbsItemId;
    private UUID reporterId;
    private LocalDate reportDate;
    private Double reportedVolume;
    private String notes;
    private OffsetDateTime createdAt;

    public DailyReportResponse(DailyReport report) {
        this.id = report.getId();
        this.wbsItemId = report.getWbsItemId();
        this.reporterId = report.getReporterId();
        this.reportDate = report.getReportDate();
        this.reportedVolume = report.getReportVolume();
        this.notes = report.getNotes();
        this.createdAt = report.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWbsItemId() {
        return wbsItemId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public Double getReportedVolume() {
        return reportedVolume;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
