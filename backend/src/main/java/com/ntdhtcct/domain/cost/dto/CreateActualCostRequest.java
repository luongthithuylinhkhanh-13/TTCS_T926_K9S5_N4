package com.ntdhtcct.domain.cost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateActualCostRequest(
        @NotBlank(message = "Tên chi phí không được để trống")
        @Size(max = 255, message = "Tên chi phí không được vượt quá 255 ký tự")
        String name,

        @NotNull(message = "Số tiền chi phí không được để trống")
        @Positive(message = "Số tiền chi phí phải lớn hơn 0")
        Double amount,

        @NotBlank(message = "Loại chi phí không được để trống")
        @Size(max = 50, message = "Loại chi phí không được vượt quá 50 ký tự")
        String costType,

        @NotNull(message = "Ngày phát sinh chi phí không được để trống")
        LocalDate incurredDate,

        UUID wbsItemId,

        UUID taskId,

        String note
) {
}
