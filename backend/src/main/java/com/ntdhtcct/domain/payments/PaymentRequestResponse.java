package com.ntdhtcct.domain.payments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentRequestResponse(
        UUID id,
        UUID projectId,
        String requestCode,
        String purpose,
        BigDecimal amount,
        UUID requesterId,
        String requesterName,
        OffsetDateTime submittedAt,
        PaymentRequestStatus status,
        String decisionReason,
        String decidedByName,
        OffsetDateTime decidedAt,
        List<PaymentRequestItemResponse> items) {

    public PaymentRequestResponse(PaymentRequest request) {
        this(
                request.getId(),
                request.getProject().getId(),
                request.getRequestCode(),
                request.getPurpose(),
                request.getAmount(),
                request.getRequester().getId(),
                request.getRequester().getFullName(),
                request.getSubmittedAt(),
                request.getStatus(),
                request.getDecisionReason(),
                request.getDecidedBy() == null ? null : request.getDecidedBy().getFullName(),
                request.getDecidedAt(),
                request.getItems().stream().map(PaymentRequestItemResponse::new).toList());
    }
}