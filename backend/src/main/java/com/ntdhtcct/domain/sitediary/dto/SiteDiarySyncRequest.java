package com.ntdhtcct.domain.sitediary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class SiteDiarySyncRequest {

    /**
     * Mã định danh đồng bộ ngoại tuyến do Client tạo (bắt buộc để đảm bảo idempotency).
     */
    @NotBlank(message = "clientSyncId không được để trống")
    private String clientSyncId;

    @NotNull(message = "Ngày ghi nhật ký không được để trống")
    private LocalDate diaryDate;

    private String weather;

    private String temperature;

    private Integer engineerCount;

    private Integer workerCount;

    private Integer crewCount;

    private String crewDetails;

    private String equipmentStatus;

    private String workingConditions;

    @NotBlank(message = "Nội dung thi công không được để trống")
    private String workSummary;

    private String issues;

    private OffsetDateTime offlineCreatedAt;

    public SiteDiarySyncRequest() {
    }

    public SiteDiarySyncRequest(
            String clientSyncId,
            LocalDate diaryDate,
            String weather,
            String temperature,
            Integer workerCount,
            String equipmentStatus,
            String workSummary,
            String issues,
            OffsetDateTime offlineCreatedAt
    ) {
        this.clientSyncId = clientSyncId;
        this.diaryDate = diaryDate;
        this.weather = weather;
        this.temperature = temperature;
        this.workerCount = workerCount;
        this.equipmentStatus = equipmentStatus;
        this.workSummary = workSummary;
        this.issues = issues;
        this.offlineCreatedAt = offlineCreatedAt;
    }

    public String getClientSyncId() {
        return clientSyncId;
    }

    public void setClientSyncId(String clientSyncId) {
        this.clientSyncId = clientSyncId;
    }

    public LocalDate getDiaryDate() {
        return diaryDate;
    }

    public void setDiaryDate(LocalDate diaryDate) {
        this.diaryDate = diaryDate;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public Integer getEngineerCount() {
        return engineerCount;
    }

    public void setEngineerCount(Integer engineerCount) {
        this.engineerCount = engineerCount;
    }

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public Integer getCrewCount() {
        return crewCount;
    }

    public void setCrewCount(Integer crewCount) {
        this.crewCount = crewCount;
    }

    public String getCrewDetails() {
        return crewDetails;
    }

    public void setCrewDetails(String crewDetails) {
        this.crewDetails = crewDetails;
    }

    public String getEquipmentStatus() {
        return equipmentStatus;
    }

    public void setEquipmentStatus(String equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
    }

    public String getWorkingConditions() {
        return workingConditions;
    }

    public void setWorkingConditions(String workingConditions) {
        this.workingConditions = workingConditions;
    }

    public String getWorkSummary() {
        return workSummary;
    }

    public void setWorkSummary(String workSummary) {
        this.workSummary = workSummary;
    }

    public String getIssues() {
        return issues;
    }

    public void setIssues(String issues) {
        this.issues = issues;
    }

    public OffsetDateTime getOfflineCreatedAt() {
        return offlineCreatedAt;
    }

    public void setOfflineCreatedAt(OffsetDateTime offlineCreatedAt) {
        this.offlineCreatedAt = offlineCreatedAt;
    }
}
