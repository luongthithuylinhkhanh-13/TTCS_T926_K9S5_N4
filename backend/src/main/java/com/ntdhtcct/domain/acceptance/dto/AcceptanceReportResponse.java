package com.ntdhtcct.domain.acceptance.dto;

import com.ntdhtcct.domain.acceptance.AcceptanceReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO trả về thông tin phiếu nghiệm thu theo đợt
 */
public class AcceptanceReportResponse {

    private UUID id;
    private UUID projectId;
    private String reportNumber;
    private String name;
    private Integer periodNumber;
    private LocalDate acceptanceDate;
    private String status;
    private UUID creatorId;
    private String notes;
    private String location;
    private String ownerRepresentativeName;
    private String supervisorRepresentativeName;
    private String contractorRepresentativeName;
    private UUID acceptanceRecordId;
    private BigDecimal totalAmount;
    private BigDecimal retentionRate;
    private BigDecimal retentionAmount;
    private int itemCount;
    private List<AcceptanceItemResponse> items = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AcceptanceReportResponse() {
    }

    public static AcceptanceReportResponse from(AcceptanceReport report) {
        AcceptanceReportResponse res = new AcceptanceReportResponse();
        res.setId(report.getId());
        res.setProjectId(report.getProjectId());
        res.setReportNumber(report.getReportNumber());
        res.setName(report.getName());
        res.setPeriodNumber(report.getPeriodNumber());
        res.setAcceptanceDate(report.getAcceptanceDate());
        res.setStatus(report.getStatus());
        res.setCreatorId(report.getCreatorId());
        res.setNotes(report.getNotes());
        res.setLocation(report.getLocation());
        res.setOwnerRepresentativeName(report.getOwnerRepresentativeName());
        res.setSupervisorRepresentativeName(report.getSupervisorRepresentativeName());
        res.setContractorRepresentativeName(report.getContractorRepresentativeName());
        res.setAcceptanceRecordId(report.getAcceptanceRecordId());
        res.setTotalAmount(report.getTotalAmount());
        res.setRetentionRate(report.getRetentionRate());
        res.setRetentionAmount(report.calculateRetentionAmount());
        if (report.getItems() != null) {
            res.setItemCount(report.getItems().size());
            res.setItems(report.getItems().stream()
                    .map(AcceptanceItemResponse::from)
                    .collect(Collectors.toList()));
        } else {
            res.setItemCount(0);
        }
        res.setCreatedAt(report.getCreatedAt());
        res.setUpdatedAt(report.getUpdatedAt());
        return res;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
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

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwnerRepresentativeName() {
        return ownerRepresentativeName;
    }

    public void setOwnerRepresentativeName(String ownerRepresentativeName) {
        this.ownerRepresentativeName = ownerRepresentativeName;
    }

    public String getSupervisorRepresentativeName() {
        return supervisorRepresentativeName;
    }

    public void setSupervisorRepresentativeName(String supervisorRepresentativeName) {
        this.supervisorRepresentativeName = supervisorRepresentativeName;
    }

    public String getContractorRepresentativeName() {
        return contractorRepresentativeName;
    }

    public void setContractorRepresentativeName(String contractorRepresentativeName) {
        this.contractorRepresentativeName = contractorRepresentativeName;
    }

    public UUID getAcceptanceRecordId() {
        return acceptanceRecordId;
    }

    public void setAcceptanceRecordId(UUID acceptanceRecordId) {
        this.acceptanceRecordId = acceptanceRecordId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public List<AcceptanceItemResponse> getItems() {
        return items;
    }

    public void setItems(List<AcceptanceItemResponse> items) {
        this.items = items;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getRetentionRate() {
        return retentionRate;
    }

    public void setRetentionRate(BigDecimal retentionRate) {
        this.retentionRate = retentionRate;
    }

    public BigDecimal getRetentionAmount() {
        return retentionAmount;
    }

    public void setRetentionAmount(BigDecimal retentionAmount) {
        this.retentionAmount = retentionAmount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
