package com.ntdhtcct.dto;

import com.ntdhtcct.entity.DependencyType;
import com.ntdhtcct.entity.TaskDependency;

import java.time.OffsetDateTime;

/**
 * DTO trả về thông tin mối quan hệ phụ thuộc giữa 2 công việc.
 */
public class TaskDependencyResponse {

    private Long id;
    private Long projectId;
    private Long predecessorId;
    private String predecessorCode;
    private String predecessorName;
    private Long successorId;
    private String successorCode;
    private String successorName;
    private DependencyType type;
    private String typeName;
    private Double lag;
    private OffsetDateTime createdAt;

    public TaskDependencyResponse() {
    }

    public static TaskDependencyResponse fromEntity(TaskDependency dep) {
        TaskDependencyResponse res = new TaskDependencyResponse();
        res.setId(dep.getId());
        res.setProjectId(dep.getProject() != null ? dep.getProject().getId() : null);
        if (dep.getPredecessor() != null) {
            res.setPredecessorId(dep.getPredecessor().getId());
            res.setPredecessorCode(dep.getPredecessor().getCode());
            res.setPredecessorName(dep.getPredecessor().getName());
        }
        if (dep.getSuccessor() != null) {
            res.setSuccessorId(dep.getSuccessor().getId());
            res.setSuccessorCode(dep.getSuccessor().getCode());
            res.setSuccessorName(dep.getSuccessor().getName());
        }
        res.setType(dep.getType());
        res.setTypeName(dep.getType() != null ? dep.getType().getVietnameseName() : null);
        res.setLag(dep.getLag());
        res.setCreatedAt(dep.getCreatedAt());
        return res;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(Long predecessorId) {
        this.predecessorId = predecessorId;
    }

    public String getPredecessorCode() {
        return predecessorCode;
    }

    public void setPredecessorCode(String predecessorCode) {
        this.predecessorCode = predecessorCode;
    }

    public String getPredecessorName() {
        return predecessorName;
    }

    public void setPredecessorName(String predecessorName) {
        this.predecessorName = predecessorName;
    }

    public Long getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(Long successorId) {
        this.successorId = successorId;
    }

    public String getSuccessorCode() {
        return successorCode;
    }

    public void setSuccessorCode(String successorCode) {
        this.successorCode = successorCode;
    }

    public String getSuccessorName() {
        return successorName;
    }

    public void setSuccessorName(String successorName) {
        this.successorName = successorName;
    }

    public DependencyType getType() {
        return type;
    }

    public void setType(DependencyType type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Double getLag() {
        return lag;
    }

    public void setLag(Double lag) {
        this.lag = lag;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
