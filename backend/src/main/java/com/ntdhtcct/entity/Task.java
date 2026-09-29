package com.ntdhtcct.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * T-19 (NTDHTCT-145): Entity ánh xạ bảng `tasks`.
 * Đại diện cho công việc/hạng mục thi công với các chỉ số tiến độ CPM (ES, EF, Duration...).
 */
@Entity
@Table(name = "tasks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tasks_project_code", columnNames = {"project_id", "code"})
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "duration", nullable = false)
    private Double duration = 0.0;

    /**
     * T-19: Thời điểm khởi sớm (Early Start - ES)
     */
    @Column(name = "early_start")
    private Double earlyStart;

    /**
     * T-19: Thời điểm kết thúc sớm (Early Finish - EF = ES + Duration)
     */
    @Column(name = "early_finish")
    private Double earlyFinish;

    @Column(name = "late_start")
    private Double lateStart;

    @Column(name = "late_finish")
    private Double lateFinish;

    @Column(name = "total_float")
    private Double totalFloat;

    @Column(name = "free_float")
    private Double freeFloat;

    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical = false;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "TODO";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Task() {
    }

    public Task(Project project, String code, String name, Double duration) {
        this.project = project;
        this.code = code;
        this.name = name;
        this.duration = duration != null ? duration : 0.0;
        this.status = "TODO";
        this.isCritical = false;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.duration == null) {
            this.duration = 0.0;
        }
        if (this.isCritical == null) {
            this.isCritical = false;
        }
        if (this.status == null) {
            this.status = "TODO";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
