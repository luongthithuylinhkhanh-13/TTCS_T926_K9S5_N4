package com.ntdhtcct.domain.payments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestItemSubmission(
        @NotNull(message = "Hạng mục nghiệm thu không được để trống")
        UUID acceptanceItemId,
        @NotNull(message = "Khối lượng đề nghị không được để trống")
        @DecimalMin(value = "0.01", message = "Khối lượng đề nghị phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2, message = "Khối lượng tối đa 13 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal quantity) {
}
