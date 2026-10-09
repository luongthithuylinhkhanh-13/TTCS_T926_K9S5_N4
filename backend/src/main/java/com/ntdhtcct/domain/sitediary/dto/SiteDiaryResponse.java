package com.ntdhtcct.domain.sitediary.dto;

import com.ntdhtcct.domain.sitediary.SiteDiary;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SiteDiaryResponse {

    private UUID id;
    private UUID projectId;
    private UUID reporterId;
    private LocalDate diaryDate;
    private String weather;
    private String temperature;
    private Integer workerCount;
    private String equipmentStatus;
    private String workSummary;
    private String issues;
    private String clientSyncId;
    private String syncStatus;
    private OffsetDateTime offlineCreatedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public SiteDiaryResponse() {
    }

    public static SiteDiaryResponse from(SiteDiary diary) {
        SiteDiaryResponse res = new SiteDiaryResponse();
        res.setId(diary.getId());
        res.setProjectId(diary.getProjectId());
        res.setReporterId(diary.getReporterId());
        res.setDiaryDate(diary.getDiaryDate());
        res.setWeather(diary.getWeather());
        res.setTemperature(diary.getTemperature());
        res.setWorkerCount(diary.getWorkerCount());
        res.setEquipmentStatus(diary.getEquipmentStatus());
        res.setWorkSummary(diary.getWorkSummary());
        res.setIssues(diary.getIssues());
        res.setClientSyncId(diary.getClientSyncId());
        res.setSyncStatus(diary.getSyncStatus());
        res.setOfflineCreatedAt(diary.getOfflineCreatedAt());
        res.setCreatedAt(diary.getCreatedAt());
        res.setUpdatedAt(diary.getUpdatedAt());
        return res;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
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

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public String getEquipmentStatus() {
        return equipmentStatus;
    }

    public void setEquipmentStatus(String equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
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

    public String getClientSyncId() {
        return clientSyncId;
    }

    public void setClientSyncId(String clientSyncId) {
        this.clientSyncId = clientSyncId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public OffsetDateTime getOfflineCreatedAt() {
        return offlineCreatedAt;
    }

    public void setOfflineCreatedAt(OffsetDateTime offlineCreatedAt) {
        this.offlineCreatedAt = offlineCreatedAt;
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
