package com.ntdhtcct.domain.sitediary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SiteDiaryUpdateRequest(
        @NotNull(message = "Ngày ghi nhật ký không được để trống")
        LocalDate diaryDate,
        String weather,
        String temperature,
        Integer engineerCount,
        Integer workerCount,
        Integer crewCount,
        String crewDetails,
        String equipmentStatus,
        String workingConditions,
        @NotBlank(message = "Nội dung thi công không được để trống")
        String workSummary,
        String issues
) {
}
