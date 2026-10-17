package com.ntdhtcct.domain.cost;

import com.ntdhtcct.domain.wbs.Task;
import com.ntdhtcct.domain.wbs.WbsItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "actual_costs")
public class ActualCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID dự án không được để trống")
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wbs_item_id")
    private WbsItem wbsItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @NotBlank(message = "Tên chi phí không được để trống")
    @Size(max = 255, message = "Tên chi phí không được vượt quá 255 ký tự")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull(message = "Số tiền chi phí không được để trống")
    @Positive(message = "Số tiền chi phí phải lớn hơn 0")
    @Column(name = "amount", nullable = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double amount;

    @NotBlank(message = "Loại chi phí không được để trống")
    @Size(max = 50, message = "Loại chi phí không được vượt quá 50 ký tự")
    @Column(name = "cost_type", nullable = false, length = 50)
    private String costType;

    @NotNull(message = "Ngày phát sinh chi phí không được để trống")
    @Column(name = "incurred_date", nullable = false)
    private LocalDate incurredDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ActualCost() {
    }

    public ActualCost(
            UUID projectId,
            WbsItem wbsItem,
            Task task,
            String name,
            Double amount,
            String costType,
            LocalDate incurredDate,
            String note,
            String createdBy
    ) {
        this.projectId = projectId;
        this.wbsItem = wbsItem;
        this.task = task;
        this.name = name;
        this.amount = amount;
        this.costType = costType;
        this.incurredDate = incurredDate;
        this.note = note;
        this.createdBy = createdBy;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public WbsItem getWbsItem() {
        return wbsItem;
    }

    public void setWbsItem(WbsItem wbsItem) {
        this.wbsItem = wbsItem;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCostType() {
        return costType;
    }

    public void setCostType(String costType) {
        this.costType = costType;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public void setIncurredDate(LocalDate incurredDate) {
        this.incurredDate = incurredDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
