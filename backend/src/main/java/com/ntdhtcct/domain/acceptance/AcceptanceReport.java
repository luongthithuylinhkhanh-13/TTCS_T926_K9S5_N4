package com.ntdhtcct.domain.acceptance;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Thực thể Phiếu nghiệm thu theo đợt (Acceptance Report)
 * Phục vụ Epic E-06 và Story S-36 (NTDHTCT-220):
 *   - NTDHTCT-268 (T-83): Tạo phiếu nghiệm thu theo từng đợt
 *   - NTDHTCT-269 (T-84): Chọn hạng mục và nhập khối lượng nghiệm thu
 *   - NTDHTCT-270 (T-85): Tự động tính giá trị nghiệm thu của phiếu
 */
@Entity
@Table(name = "acceptance_reports")
public class AcceptanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID dự án không được để trống")
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @NotBlank(message = "Mã số biên bản nghiệm thu không được để trống")
    @Column(name = "report_number", nullable = false, length = 50)
    private String reportNumber;

    @NotBlank(message = "Tên phiếu nghiệm thu không được để trống")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull(message = "Số đợt nghiệm thu không được để trống")
    @Min(value = 1, message = "Số đợt nghiệm thu phải từ 1 trở lên")
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber = 1;

    @NotNull(message = "Ngày nghiệm thu không được để trống")
    @Column(name = "acceptance_date", nullable = false)
    private LocalDate acceptanceDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT"; // DRAFT | SUBMITTED | APPROVED | REJECTED

    @Column(name = "creator_id")
    private UUID creatorId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "acceptanceReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcceptanceItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AcceptanceReport() {
    }

    public AcceptanceReport(
            UUID projectId,
            String reportNumber,
            String name,
            Integer periodNumber,
            LocalDate acceptanceDate,
            String status,
            UUID creatorId,
            String notes
    ) {
        this.projectId = projectId;
        this.reportNumber = reportNumber;
        this.name = name;
        this.periodNumber = periodNumber != null ? periodNumber : 1;
        this.acceptanceDate = acceptanceDate;
        this.status = status != null ? status : "DRAFT";
        this.creatorId = creatorId;
        this.notes = notes;
        this.totalAmount = BigDecimal.ZERO;
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
        if (this.status == null) {
            this.status = "DRAFT";
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void addItem(AcceptanceItem item) {
        items.add(item);
        item.setAcceptanceReport(this);
    }

    public void removeItem(AcceptanceItem item) {
        items.remove(item);
        item.setAcceptanceReport(null);
    }

    public void clearItems() {
        for (AcceptanceItem item : new ArrayList<>(items)) {
            removeItem(item);
        }
    }

    /**
     * T-85 (NTDHTCT-270): Tự động tính toán tổng giá trị nghiệm thu của toàn bộ phiếu
     */
    public void calculateTotalAmount() {
        BigDecimal sum = BigDecimal.ZERO;
        for (AcceptanceItem item : items) {
            item.calculateAmount();
            if (item.getAmount() != null) {
                sum = sum.add(item.getAmount());
            }
        }
        this.totalAmount = sum;
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

    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }

    public LocalDate getAcceptanceDate() {
        return acceptanceDate;
    }

    public void setAcceptanceDate(LocalDate acceptanceDate) {
        this.acceptanceDate = acceptanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<AcceptanceItem> getItems() {
        return items;
    }

    public void setItems(List<AcceptanceItem> items) {
        this.items = items;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
