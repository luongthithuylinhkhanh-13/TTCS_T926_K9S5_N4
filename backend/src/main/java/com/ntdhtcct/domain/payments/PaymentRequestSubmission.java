package com.ntdhtcct.domain.payments;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequestSubmission(
        @NotBlank(message = "Nội dung đề nghị không được để trống")
        @Size(max = 2000, message = "Nội dung đề nghị không được vượt quá 2000 ký tự")
        String purpose,
        @NotNull(message = "Số tiền đề nghị không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền đề nghị phải lớn hơn 0")
        @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal amount) {
}