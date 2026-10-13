package com.ntdhtcct.domain.sitediary.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SiteDiaryLockRequest(
        @NotNull(message = "Ngày khóa sổ không được để trống")
        LocalDate lockDate,
        String reason
) {
}
