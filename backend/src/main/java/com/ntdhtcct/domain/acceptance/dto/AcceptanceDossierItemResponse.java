package com.ntdhtcct.domain.acceptance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AcceptanceDossierItemResponse(
        UUID id,
        String itemName,
        String wbsCode,
        String location,
        String unit,
        BigDecimal plannedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal cumulativeQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String notes
) {
}
