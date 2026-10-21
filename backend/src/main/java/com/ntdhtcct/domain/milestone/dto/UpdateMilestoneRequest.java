package com.ntdhtcct.domain.milestone.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateMilestoneRequest(
        UUID categoryId,

        @Size(max = 255, message = "Tên mốc tiến độ không được vượt quá 255 ký tự")
        String name,

        LocalDate targetDate,

        String description
) {
}
