package com.ntdhtcct.domain.sitediary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thực thể quản lý khóa sổ nhật ký công trường theo ngày (S-24: T-64).
 * Quản lý theo từng cặp (projectId, lockDate).
 */
@Entity
@Table(
        name = "site_diary_locks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_site_diary_locks_project_date", columnNames = {"project_id", "lock_date"})
        }
)
public class SiteDiaryLock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID dự án không được để trống")
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @NotNull(message = "Ngày khóa sổ không được để trống")
    @Column(name = "lock_date", nullable = false)
    private LocalDate lockDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "LOCKED";

    @NotNull(message = "Người khóa không được để trống")
    @Column(name = "locked_by", nullable = false)
    private UUID lockedBy;

    @Column(name = "locked_at", nullable = false)
    private OffsetDateTime lockedAt;

    @Column(name = "lock_reason", columnDefinition = "TEXT")
    private String lockReason;

    @Column(name = "unlocked_by")
    private UUID unlockedBy;

    @Column(name = "unlocked_at")
    private OffsetDateTime unlockedAt;

    @Column(name = "unlock_reason", columnDefinition = "TEXT")
    private String unlockReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SiteDiaryLock() {
    }

    public SiteDiaryLock(UUID projectId, LocalDate lockDate, UUID lockedBy, String lockReason) {
        this.projectId = projectId;
        this.lockDate = lockDate;
        this.lockedBy = lockedBy;
        this.lockReason = lockReason;
        this.status = "LOCKED";
        this.lockedAt = OffsetDateTime.now();
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
        if (this.lockedAt == null) {
            this.lockedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isLocked() {
        return "LOCKED".equalsIgnoreCase(this.status);
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

    public LocalDate getLockDate() {
        return lockDate;
    }

    public void setLockDate(LocalDate lockDate) {
        this.lockDate = lockDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(UUID lockedBy) {
        this.lockedBy = lockedBy;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(OffsetDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public UUID getUnlockedBy() {
        return unlockedBy;
    }

    public void setUnlockedBy(UUID unlockedBy) {
        this.unlockedBy = unlockedBy;
    }

    public OffsetDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(OffsetDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public String getUnlockReason() {
        return unlockReason;
    }

    public void setUnlockReason(String unlockReason) {
        this.unlockReason = unlockReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
