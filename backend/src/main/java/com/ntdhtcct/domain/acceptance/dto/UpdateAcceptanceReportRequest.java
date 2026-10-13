package com.ntdhtcct.domain.acceptance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO cập nhật phiếu nghiệm thu (T-83, T-84, T-85)
 */
public class UpdateAcceptanceReportRequest {

    @NotBlank(message = "Tên phiếu nghiệm thu không được để trống")
    private String name;

    @NotNull(message = "Ngày nghiệm thu không được để trống")
    private LocalDate acceptanceDate;

    private String status;
    private String notes;

    @Valid
    private List<AcceptanceItemRequest> items = new ArrayList<>();

    public UpdateAcceptanceReportRequest() {
    }

    public UpdateAcceptanceReportRequest(
            String name,
            LocalDate acceptanceDate,
            String status,
            String notes,
            List<AcceptanceItemRequest> items
    ) {
        this.name = name;
        this.acceptanceDate = acceptanceDate;
        this.status = status;
        this.notes = notes;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
