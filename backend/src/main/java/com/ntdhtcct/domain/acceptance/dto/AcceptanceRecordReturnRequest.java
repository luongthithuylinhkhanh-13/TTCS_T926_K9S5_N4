package com.ntdhtcct.domain.acceptance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptanceRecordReturnRequest(
        @NotBlank(message = "Vui lòng nhập lý do trả lại phiếu nghiệm thu")
        @Size(max = 2000, message = "Lý do trả lại không được vượt quá 2000 ký tự")
        String comment
) {
}