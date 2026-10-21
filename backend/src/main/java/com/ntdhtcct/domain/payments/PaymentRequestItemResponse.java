package com.ntdhtcct.domain.payments;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestItemResponse(
        UUID acceptanceItemId,
        String itemName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount) {

    public PaymentRequestItemResponse(PaymentRequestItem item) {
        this(
                item.getAcceptanceItem().getId(),
                item.getItemName(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getAmount());
    }
}
