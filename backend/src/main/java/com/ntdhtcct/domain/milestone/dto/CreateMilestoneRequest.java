package com.ntdhtcct.domain.milestone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMilestoneRequest(
        @NotNull(message = "Hạng mục không được để trống")
        UUID categoryId,

        @NotBlank(message = "Tên mốc tiến độ không được để trống")
        @Size(max = 255, message = "Tên mốc tiến độ không được vượt quá 255 ký tự")
        String name,

        @NotNull(message = "Ngày bắt buộc của mốc không được để trống")
        LocalDate targetDate,

        String description
) {
}
