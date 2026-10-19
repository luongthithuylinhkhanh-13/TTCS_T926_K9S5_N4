package com.ntdhtcct.domain.project.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSiteLocationRequest(
        @NotNull(message = "Vĩ độ không được để trống")
        @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng từ -90 đến 90 độ")
        @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng từ -90 đến 90 độ")
        Double latitude,

        @NotNull(message = "Kinh độ không được để trống")
        @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng từ -180 đến 180 độ")
        @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng từ -180 đến 180 độ")
        Double longitude,

        @NotNull(message = "Bán kính công trường không được để trống")
        @Positive(message = "Bán kính công trường phải lớn hơn 0 mét")
        Double radiusMeters
) {
}
