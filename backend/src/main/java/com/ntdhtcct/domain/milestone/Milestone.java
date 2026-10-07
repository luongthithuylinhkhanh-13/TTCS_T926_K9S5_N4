package com.ntdhtcct.domain.milestone;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ntdhtcct.domain.wbs.WbsItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID dự án không được để trống")
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @NotNull(message = "Hạng mục gắn mốc không được để trống")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private WbsItem category;

    @NotBlank(message = "Tên mốc tiến độ không được để trống")
    @Size(max = 255, message = "Tên mốc tiến độ không được vượt quá 255 ký tự")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull(message = "Ngày bắt buộc của mốc không được để trống")
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Milestone() {
    }

    public Milestone(
            Long projectId,
            WbsItem category,
            String name,
            LocalDate targetDate,
            String description
    ) {
        this.projectId = projectId;
        this.category = category;
        this.name = name;
        this.targetDate = targetDate;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public WbsItem getCategory() {
        return category;
    }

    public void setCategory(WbsItem category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}