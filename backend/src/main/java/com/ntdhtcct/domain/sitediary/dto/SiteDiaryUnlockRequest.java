package com.ntdhtcct.domain.sitediary.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SiteDiaryUnlockRequest(
        @NotNull(message = "Ngày mở khóa không được để trống")
        LocalDate lockDate,
        String reason
) {
}
