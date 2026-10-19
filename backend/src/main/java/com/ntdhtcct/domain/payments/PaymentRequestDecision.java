package com.ntdhtcct.domain.payments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRequestDecision(
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(max = 2000, message = "Lý do từ chối không được vượt quá 2000 ký tự")
        String reason) {
}