package com.ntdhtcct.domain.acceptance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thực thể Chi tiết hạng mục nghiệm thu (Acceptance Item)
 * Phục vụ:
 *   - NTDHTCT-269 (T-84): Chọn hạng mục và nhập khối lượng nghiệm thu
 *   - NTDHTCT-270 (T-85): Tự động tính giá trị nghiệm thu: amount = accepted_quantity * unit_price
 */
@Entity
@Table(name = "acceptance_items")
public class AcceptanceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_report_id", nullable = false)
    private AcceptanceReport acceptanceReport;

    @Column(name = "wbs_item_id")
    private UUID wbsItemId;

    @Column(name = "task_id")
    private UUID taskId;

    @NotBlank(message = "Tên hạng mục không được để trống")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "planned_quantity", precision = 15, scale = 2)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;

    @NotNull(message = "Khối lượng nghiệm thu không được để trống")
    @PositiveOrZero(message = "Khối lượng nghiệm thu không được âm")
    @Column(name = "accepted_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal acceptedQuantity = BigDecimal.ZERO;

    @NotNull(message = "Đơn giá không được để trống")
    @PositiveOrZero(message = "Đơn giá không được âm")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "cumulative_quantity", precision = 15, scale = 2)
    private BigDecimal cumulativeQuantity = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AcceptanceItem() {
    }

    public AcceptanceItem(
            UUID wbsItemId,
            UUID taskId,
            String itemName,
            String unit,
            BigDecimal plannedQuantity,
            BigDecimal acceptedQuantity,
            BigDecimal unitPrice,
            BigDecimal cumulativeQuantity,
            String notes
    ) {
        this.wbsItemId = wbsItemId;
        this.taskId = taskId;
        this.itemName = itemName;
        this.unit = unit;
        this.plannedQuantity = plannedQuantity != null ? plannedQuantity : BigDecimal.ZERO;
        this.acceptedQuantity = acceptedQuantity != null ? acceptedQuantity : BigDecimal.ZERO;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.cumulativeQuantity = cumulativeQuantity != null ? cumulativeQuantity : BigDecimal.ZERO;
        this.notes = notes;
        calculateAmount();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        calculateAmount();
    }

    /**
     * T-85 (NTDHTCT-270): Tự động tính giá trị nghiệm thu của từng hạng mục
     * amount = acceptedQuantity * unitPrice
     */
    public void calculateAmount() {
        BigDecimal qty = this.acceptedQuantity != null ? this.acceptedQuantity : BigDecimal.ZERO;
        BigDecimal price = this.unitPrice != null ? this.unitPrice : BigDecimal.ZERO;
        this.amount = qty.multiply(price);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AcceptanceReport getAcceptanceReport() {
        return acceptanceReport;
    }

    public void setAcceptanceReport(AcceptanceReport acceptanceReport) {
        this.acceptanceReport = acceptanceReport;
    }

    public UUID getWbsItemId() {
        return wbsItemId;
    }

    public void setWbsItemId(UUID wbsItemId) {
        this.wbsItemId = wbsItemId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(BigDecimal plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(BigDecimal acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
        calculateAmount();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateAmount();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCumulativeQuantity() {
        return cumulativeQuantity;
    }

    public void setCumulativeQuantity(BigDecimal cumulativeQuantity) {
        this.cumulativeQuantity = cumulativeQuantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
