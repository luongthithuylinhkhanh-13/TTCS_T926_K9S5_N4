package com.ntdhtcct.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * T-19 (NTDHTCT-145): DTO trả về kết quả tính toán tiến độ Forward Pass (ES & EF).
 */
public class ScheduleCalculationResponse {

    private Long projectId;
    private Double projectDuration;
    private Integer totalTasks;
    private List<TaskScheduleDto> schedule;
    private OffsetDateTime calculatedAt;

    public ScheduleCalculationResponse() {
    }

    public ScheduleCalculationResponse(Long projectId, Double projectDuration, Integer totalTasks, List<TaskScheduleDto> schedule) {
        this.projectId = projectId;
        this.projectDuration = projectDuration;
        this.totalTasks = totalTasks;
        this.schedule = schedule;
        this.calculatedAt = OffsetDateTime.now();
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Double getProjectDuration() {
        return projectDuration;
    }

    public void setProjectDuration(Double projectDuration) {
        this.projectDuration = projectDuration;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public List<TaskScheduleDto> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<TaskScheduleDto> schedule) {
        this.schedule = schedule;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(OffsetDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
