package com.ntdhtcct.domain.payments;

import com.ntdhtcct.domain.acceptance.AcceptanceItem;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestEligibleItemResponse(
        UUID id,
        UUID acceptanceReportId,
        String reportNumber,
        String itemName,
        String unit,
        BigDecimal acceptedQuantity,
        BigDecimal unitPrice,
        BigDecimal amount) {

    public PaymentRequestEligibleItemResponse(AcceptanceItem item) {
        this(
                item.getId(),
                item.getAcceptanceReport().getId(),
                item.getAcceptanceReport().getReportNumber(),
                item.getItemName(),
                item.getUnit(),
                item.getAcceptedQuantity(),
                item.getUnitPrice(),
                item.getAmount());
    }
}
