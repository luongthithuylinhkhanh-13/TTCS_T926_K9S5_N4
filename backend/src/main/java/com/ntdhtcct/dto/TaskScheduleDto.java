package com.ntdhtcct.dto;

/**
 * DTO đại diện kết quả tính toán ES, EF của từng công việc trong tiến độ.
 */
public class TaskScheduleDto {

    private Long taskId;
    private String code;
    private String name;
    private Double duration;
    private Double earlyStart;
    private Double earlyFinish;

    public TaskScheduleDto() {
    }

    public TaskScheduleDto(Long taskId, String code, String name, Double duration, Double earlyStart, Double earlyFinish) {
        this.taskId = taskId;
        this.code = code;
        this.name = name;
        this.duration = duration;
        this.earlyStart = earlyStart;
        this.earlyFinish = earlyFinish;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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
}
