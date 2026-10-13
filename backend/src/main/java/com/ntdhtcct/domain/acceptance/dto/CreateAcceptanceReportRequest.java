package com.ntdhtcct.domain.acceptance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO tạo mới phiếu nghiệm thu theo đợt (T-83, T-84, T-85)
 */
public class CreateAcceptanceReportRequest {

    @NotBlank(message = "Mã số biên bản nghiệm thu không được để trống")
    private String reportNumber;

    @NotBlank(message = "Tên phiếu nghiệm thu không được để trống")
    private String name;

    @NotNull(message = "Số đợt nghiệm thu không được để trống")
    @Min(value = 1, message = "Số đợt nghiệm thu phải từ 1 trở lên")
    private Integer periodNumber = 1;

    @NotNull(message = "Ngày nghiệm thu không được để trống")
    private LocalDate acceptanceDate;

    private String status = "DRAFT";
    private String notes;

    @Valid
    private List<AcceptanceItemRequest> items = new ArrayList<>();

    public CreateAcceptanceReportRequest() {
    }

    public CreateAcceptanceReportRequest(
            String reportNumber,
            String name,
            Integer periodNumber,
            LocalDate acceptanceDate,
            String status,
            String notes,
            List<AcceptanceItemRequest> items
    ) {
        this.reportNumber = reportNumber;
        this.name = name;
        this.periodNumber = periodNumber;
        this.acceptanceDate = acceptanceDate;
        this.status = status;
        this.notes = notes;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }

    public LocalDate getAcceptanceDate() {
        return acceptanceDate;
    }

    public void setAcceptanceDate(LocalDate acceptanceDate) {
        this.acceptanceDate = acceptanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<AcceptanceItemRequest> getItems() {
        return items;
    }

    public void setItems(List<AcceptanceItemRequest> items) {
        this.items = items;
    }
}
