package com.ntdhtcct.domain.acceptance.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO trả về thống kê tổng quan nghiệm thu của dự án
 */
public class AcceptanceSummaryResponse {

    private UUID projectId;
    private long totalReports;
    private long draftCount;
    private long submittedCount;
    private long approvedCount;
    private BigDecimal totalAmount;
    private BigDecimal approvedAmount;

    public AcceptanceSummaryResponse() {
    }

    public AcceptanceSummaryResponse(
            UUID projectId,
            long totalReports,
            long draftCount,
            long submittedCount,
            long approvedCount,
            BigDecimal totalAmount,
            BigDecimal approvedAmount
    ) {
        this.projectId = projectId;
        this.totalReports = totalReports;
        this.draftCount = draftCount;
        this.submittedCount = submittedCount;
        this.approvedCount = approvedCount;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.approvedAmount = approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getDraftCount() {
        return draftCount;
    }

    public void setDraftCount(long draftCount) {
        this.draftCount = draftCount;
    }

    public long getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(long submittedCount) {
        this.submittedCount = submittedCount;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }
}
