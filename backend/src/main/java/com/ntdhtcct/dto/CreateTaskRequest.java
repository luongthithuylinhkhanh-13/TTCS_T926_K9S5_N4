package com.ntdhtcct.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu tạo công việc mới cho dự án.
 */
public class CreateTaskRequest {

    @NotBlank(message = "Mã công việc không được để trống")
    @Size(max = 50, message = "Mã công việc không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên công việc không được để trống")
    @Size(max = 255, message = "Tên công việc không vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Thời lượng công việc không được để trống")
    @PositiveOrZero(message = "Thời lượng công việc phải lớn hơn hoặc bằng 0")
    private Double duration;

    private String description;

    public CreateTaskRequest() {
    }

    public CreateTaskRequest(String code, String name, Double duration, String description) {
        this.code = code;
        this.name = name;
        this.duration = duration;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
