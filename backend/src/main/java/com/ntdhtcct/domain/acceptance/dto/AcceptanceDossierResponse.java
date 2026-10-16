package com.ntdhtcct.domain.acceptance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AcceptanceDossierResponse(
        UUID id,
        UUID projectId,
        String projectCode,
        String projectName,
        String reportNumber,
        String reportName,
        Integer periodNumber,
        LocalDate acceptanceDate,
        String location,
        String result,
        String status,
        String notes,
        String ownerRepresentativeName,
        String supervisorRepresentativeName,
        String contractorRepresentativeName,
        String creatorName,
        BigDecimal totalAmount,
        List<AcceptanceDossierItemResponse> items,
        UUID acceptanceRecordId,
        String approvalRecordTitle,
        String approvalRecordDescription,
        String approvalStatus,
        String approverName,
        String approvalComment,
        OffsetDateTime approvedAt
) {
}
