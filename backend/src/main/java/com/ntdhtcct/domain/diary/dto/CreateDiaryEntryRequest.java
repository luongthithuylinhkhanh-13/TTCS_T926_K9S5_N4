package com.ntdhtcct.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDiaryEntryRequest(
        UUID wbsItemId,

        @NotNull(message = "Ngày ghi nhật ký không được để trống")
        LocalDate entryDate,

        @NotBlank(message = "Tiêu đề nhật ký không được để trống")
        @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
        String title,

        String content,

        @Size(max = 50, message = "Thông tin thời tiết không quá 50 ký tự")
        String weather
) {
}
