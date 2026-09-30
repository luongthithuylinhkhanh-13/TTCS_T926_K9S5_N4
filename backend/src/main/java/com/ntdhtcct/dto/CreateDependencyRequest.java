package com.ntdhtcct.dto;

import com.ntdhtcct.domain.DependencyType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateDependencyRequest {

    @NotNull(message = "Công việc trước (predecessorId) không được để trống")
    private UUID predecessorId;

    @NotNull(message = "Loại quan hệ phụ thuộc không được để trống")
    private DependencyType dependencyType = DependencyType.FS;

    @NotNull(message = "Độ trễ/gối đầu (lagDays) không được để trống")
    private Integer lagDays = 0;

    public CreateDependencyRequest() {
    }

    public CreateDependencyRequest(UUID predecessorId, DependencyType dependencyType, Integer lagDays) {
        this.predecessorId = predecessorId;
        this.dependencyType = dependencyType != null ? dependencyType : DependencyType.FS;
        this.lagDays = lagDays != null ? lagDays : 0;
    }

    public UUID getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(UUID predecessorId) {
        this.predecessorId = predecessorId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public Integer getLagDays() {
        return lagDays;
    }

    public void setLagDays(Integer lagDays) {
        this.lagDays = lagDays;
    }
}