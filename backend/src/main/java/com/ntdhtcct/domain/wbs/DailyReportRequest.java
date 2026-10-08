package com.ntdhtcct.domain.wbs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class DailyReportRequest {

    @NotNull(message = "WbsItemId cannot be null")
    private UUID wbsItemId;

    @NotNull(message = "Report date cannot be null")
    private LocalDate reportDate;

    @NotNull(message = "Reported volume cannot be null")
    @Min(value = 0, message = "Reported volume must be greater than or equal to 0")
    private Double reportedVolume;

    private String notes;

    public UUID getWbsItemId() {
        return wbsItemId;
    }

    public void setWbsItemId(UUID wbsItemId) {
        this.wbsItemId = wbsItemId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Double getReportedVolume() {
        return reportedVolume;
    }

    public void setReportedVolume(Double reportedVolume) {
        this.reportedVolume = reportedVolume;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
