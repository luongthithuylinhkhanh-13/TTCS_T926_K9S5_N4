package com.ntdhtcct.dto;

import com.ntdhtcct.entity.Task;

import java.time.OffsetDateTime;

/**
 * DTO trả về thông tin công việc và các chỉ số tiến độ ES/EF.
 */
public class TaskResponse {

    private Long id;
    private Long projectId;
    private String code;
    private String name;
    private Double duration;
    private Double earlyStart;
    private Double earlyFinish;
    private Double lateStart;
    private Double lateFinish;
    private Double totalFloat;
    private Double freeFloat;
    private Boolean isCritical;
    private String status;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TaskResponse() {
    }

    public static TaskResponse fromEntity(Task task) {
        TaskResponse res = new TaskResponse();
        res.setId(task.getId());
        res.setProjectId(task.getProject() != null ? task.getProject().getId() : null);
        res.setCode(task.getCode());
        res.setName(task.getName());
        res.setDuration(task.getDuration());
        res.setEarlyStart(task.getEarlyStart());
        res.setEarlyFinish(task.getEarlyFinish());
        res.setLateStart(task.getLateStart());
        res.setLateFinish(task.getLateFinish());
        res.setTotalFloat(task.getTotalFloat());
        res.setFreeFloat(task.getFreeFloat());
        res.setIsCritical(task.getIsCritical());
        res.setStatus(task.getStatus());
        res.setDescription(task.getDescription());
        res.setCreatedAt(task.getCreatedAt());
        res.setUpdatedAt(task.getUpdatedAt());
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

    public Double getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(Double earlyStart) {
        this.earlyStart = earlyStart;
    }

    public Double getEarlyFinish() {
        return earlyFinish;
    }

    public void setEarlyFinish(Double earlyFinish) {
        this.earlyFinish = earlyFinish;
    }

    public Double getLateStart() {
        return lateStart;
    }

    public void setLateStart(Double lateStart) {
        this.lateStart = lateStart;
    }

    public Double getLateFinish() {
        return lateFinish;
    }

    public void setLateFinish(Double lateFinish) {
        this.lateFinish = lateFinish;
    }

    public Double getTotalFloat() {
        return totalFloat;
    }

    public void setTotalFloat(Double totalFloat) {
        this.totalFloat = totalFloat;
    }

    public Double getFreeFloat() {
        return freeFloat;
    }

    public void setFreeFloat(Double freeFloat) {
        this.freeFloat = freeFloat;
    }

    public Boolean getIsCritical() {
        return isCritical;
    }

    public void setIsCritical(Boolean critical) {
        isCritical = critical;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
