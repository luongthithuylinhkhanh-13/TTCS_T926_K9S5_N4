package com.ntdhtcct.domain.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchUpdateBudgetRequest(
        @NotEmpty(message = "Danh sách cập nhật dự toán không được để trống")
        List<@Valid ItemBudgetEntry> items
) {
    public record ItemBudgetEntry(
            @NotNull(message = "ID hạng mục không được để trống")
            UUID itemId,

            @DecimalMin(value = "0.0", message = "Khối lượng phải lớn hơn hoặc bằng 0")
            Double quantity,

            @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
            String unit,

            @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
            Double unitPrice
    ) {
    }
}
