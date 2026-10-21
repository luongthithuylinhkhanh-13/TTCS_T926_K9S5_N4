package com.ntdhtcct.domain.acceptance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO nhận thông tin từng hạng mục nghiệm thu (T-84, T-85)
 */
public class AcceptanceItemRequest {

    private UUID wbsItemId;
    private UUID taskId;

    @NotBlank(message = "Tên hạng mục không được để trống")
    private String itemName;

    private String unit;

    @PositiveOrZero(message = "Khối lượng thiết kế không được âm")
    private BigDecimal plannedQuantity;

    @NotNull(message = "Khối lượng nghiệm thu không được để trống")
    @PositiveOrZero(message = "Khối lượng nghiệm thu không được âm")
    private BigDecimal acceptedQuantity;

    @NotNull(message = "Đơn giá không được để trống")
    @PositiveOrZero(message = "Đơn giá không được âm")
    private BigDecimal unitPrice;

    private BigDecimal cumulativeQuantity;
    private String notes;

    public AcceptanceItemRequest() {
    }

    public AcceptanceItemRequest(
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
        this.plannedQuantity = plannedQuantity;
        this.acceptedQuantity = acceptedQuantity;
        this.unitPrice = unitPrice;
        this.cumulativeQuantity = cumulativeQuantity;
        this.notes = notes;
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
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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
}
