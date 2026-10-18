package com.ntdhtcct.domain.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateBudgetItemRequest(
        @DecimalMin(value = "0.0", message = "Khối lượng phải lớn hơn hoặc bằng 0")
        Double quantity,

        @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
        String unit,

        @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
        Double unitPrice
) {
}
