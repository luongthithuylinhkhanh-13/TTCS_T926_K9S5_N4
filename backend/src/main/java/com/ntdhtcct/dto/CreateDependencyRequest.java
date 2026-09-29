package com.ntdhtcct.dto;

import com.ntdhtcct.entity.DependencyType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu tạo liên kết phụ thuộc giữa 2 công việc.
 */
public class CreateDependencyRequest {

    @NotNull(message = "ID công việc tiền nhiệm không được để trống")
    private Long predecessorId;

    @NotNull(message = "ID công việc kế nhiệm không được để trống")
    private Long successorId;

    @NotNull(message = "Loại quan hệ (FS, SS, FF, SF) không được để trống")
    private DependencyType type;

    private Double lag = 0.0;

    public CreateDependencyRequest() {
    }

    public CreateDependencyRequest(Long predecessorId, Long successorId, DependencyType type, Double lag) {
        this.predecessorId = predecessorId;
        this.successorId = successorId;
        this.type = type;
        this.lag = lag != null ? lag : 0.0;
    }

    public Long getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(Long predecessorId) {
        this.predecessorId = predecessorId;
    }

    public Long getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(Long successorId) {
        this.successorId = successorId;
    }

    public DependencyType getType() {
        return type;
    }

    public void setType(DependencyType type) {
        this.type = type;
    }

    public Double getLag() {
        return lag;
    }

    public void setLag(Double lag) {
        this.lag = lag;
    }
}
