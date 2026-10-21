package com.ntdhtcct.domain.acceptance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AcceptanceRecordRequest(
        @NotBlank(message = "Tên phiếu nghiệm thu không được để trống")
        @Size(max = 255, message = "Tên phiếu nghiệm thu không được vượt quá 255 ký tự")
        String title,

        @NotNull(message = "Ngày nghiệm thu không được để trống")
        LocalDate inspectionDate,

        @NotBlank(message = "Nội dung nghiệm thu không được để trống")
        String description
) {
}
