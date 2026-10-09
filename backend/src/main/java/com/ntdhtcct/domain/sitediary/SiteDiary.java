package com.ntdhtcct.domain.sitediary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thực thể Nhật ký công trường (Site Diary) phục vụ Epic E-05 và Story S-30.
 * Hỗ trợ đồng bộ ngoại tuyến với trường clientSyncId để bảo đảm tính Idempotent.
 */
@Entity
@Table(name = "site_diaries")
public class SiteDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID dự án không được để trống")
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @NotNull(message = "Ngày ghi nhật ký không được để trống")
    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(name = "weather", length = 100)
    private String weather;

    @Column(name = "temperature", length = 50)
    private String temperature;

    @Column(name = "worker_count")
    private Integer workerCount;

    @Column(name = "equipment_status", columnDefinition = "TEXT")
    private String equipmentStatus;

    @NotBlank(message = "Nội dung thi công không được để trống")
    @Column(name = "work_summary", nullable = false, columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "issues", columnDefinition = "TEXT")
    private String issues;

    /**
     * Mã định danh đồng bộ duy nhất sinh từ phía Client khi offline.
     * Dùng để ngăn chặn gửi trùng lặp (Idempotency) khi kết nối mạng chập chờn.
     */
    @Column(name = "client_sync_id", length = 100, unique = true)
    private String clientSyncId;

    @Column(name = "sync_status", nullable = false, length = 50)
    private String syncStatus = "SYNCED";

    /**
     * Thời điểm bản ghi được tạo ở Client khi đang mất mạng.
     */
    @Column(name = "offline_created_at")
    private OffsetDateTime offlineCreatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SiteDiary() {
    }

    public SiteDiary(
            UUID projectId,
            UUID reporterId,
            LocalDate diaryDate,
            String weather,
            String temperature,
            Integer workerCount,
            String equipmentStatus,
            String workSummary,
            String issues,
            String clientSyncId,
            OffsetDateTime offlineCreatedAt
    ) {
        this.projectId = projectId;
        this.reporterId = reporterId;
        this.diaryDate = diaryDate;
        this.weather = weather;
        this.temperature = temperature;
        this.workerCount = workerCount;
        this.equipmentStatus = equipmentStatus;
        this.workSummary = workSummary;
        this.issues = issues;
        this.clientSyncId = clientSyncId;
        this.offlineCreatedAt = offlineCreatedAt;
        this.syncStatus = "SYNCED";
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
