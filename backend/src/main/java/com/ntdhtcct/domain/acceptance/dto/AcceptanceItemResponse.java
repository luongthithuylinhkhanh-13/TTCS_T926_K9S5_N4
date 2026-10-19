package com.ntdhtcct.domain.acceptance.dto;

import com.ntdhtcct.domain.acceptance.AcceptanceItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin chi tiết từng hạng mục nghiệm thu
 */
public class AcceptanceItemResponse {

    private UUID id;
    private UUID wbsItemId;
    private UUID taskId;
    private String itemName;
    private String unit;
    private BigDecimal plannedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal amount; // T-85: acceptedQuantity * unitPrice
    private BigDecimal cumulativeQuantity;
    private String notes;
    private OffsetDateTime createdAt;

    public AcceptanceItemResponse() {
    }

    public static AcceptanceItemResponse from(AcceptanceItem item) {
        AcceptanceItemResponse res = new AcceptanceItemResponse();
        res.setId(item.getId());
        res.setWbsItemId(item.getWbsItemId());
        res.setTaskId(item.getTaskId());
        res.setItemName(item.getItemName());
        res.setUnit(item.getUnit());
        res.setPlannedQuantity(item.getPlannedQuantity());
        res.setAcceptedQuantity(item.getAcceptedQuantity());
        res.setUnitPrice(item.getUnitPrice());
        res.setAmount(item.getAmount());
        res.setCumulativeQuantity(item.getCumulativeQuantity());
        res.setNotes(item.getNotes());
        res.setCreatedAt(item.getCreatedAt());
        return res;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
