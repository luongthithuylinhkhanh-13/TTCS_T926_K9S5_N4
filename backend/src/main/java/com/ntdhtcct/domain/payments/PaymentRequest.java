package com.ntdhtcct.domain.payments;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "request_code", nullable = false, length = 50)
    private String requestCode;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentRequestStatus status = PaymentRequestStatus.PENDING;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "paymentRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentRequestItem> items = new ArrayList<>();

    protected PaymentRequest() {
    }

    public PaymentRequest(
            Project project,
            String requestCode,
            String purpose,
            BigDecimal amount,
            User requester) {
        this.project = project;
        this.requestCode = requestCode;
        this.purpose = purpose;
        this.amount = amount;
        this.requester = requester;
        this.submittedAt = OffsetDateTime.now();
        this.status = PaymentRequestStatus.DRAFT;
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public String getPurpose() {
        return purpose;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public User getRequester() {
        return requester;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public PaymentRequestStatus getStatus() {
        return status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public List<PaymentRequestItem> getItems() {
        return items;
    }

    public void replaceItems(List<PaymentRequestItem> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            this.items.clear();
            return;
        }

        Map<UUID, PaymentRequestItem> newItemsByAcceptanceId = new HashMap<>();
        List<PaymentRequestItem> unkeyedItems = new ArrayList<>();
        for (PaymentRequestItem item : newItems) {
            UUID acceptanceId = (item.getAcceptanceItem() != null) ? item.getAcceptanceItem().getId() : null;
            if (acceptanceId != null) {
                newItemsByAcceptanceId.put(acceptanceId, item);
            } else {
                unkeyedItems.add(item);
            }
        }

        Iterator<PaymentRequestItem> iterator = this.items.iterator();
        while (iterator.hasNext()) {
            PaymentRequestItem existing = iterator.next();
            UUID acceptanceId = existing.getAcceptanceItem() != null ? existing.getAcceptanceItem().getId() : null;
            if (acceptanceId != null && newItemsByAcceptanceId.containsKey(acceptanceId)) {
                PaymentRequestItem updated = newItemsByAcceptanceId.remove(acceptanceId);
                existing.update(updated.getQuantity(), updated.getUnitPrice(), updated.getAmount(), updated.getItemName(), updated.getUnit());
            } else if (acceptanceId == null && !unkeyedItems.isEmpty()) {
                PaymentRequestItem unkeyed = unkeyedItems.remove(0);
                existing.update(unkeyed.getQuantity(), unkeyed.getUnitPrice(), unkeyed.getAmount(), unkeyed.getItemName(), unkeyed.getUnit());
            } else {
                existing.setPaymentRequest(null);
                iterator.remove();
            }
        }

        for (PaymentRequestItem newItem : newItemsByAcceptanceId.values()) {
            newItem.setPaymentRequest(this);
            this.items.add(newItem);
        }
        for (PaymentRequestItem unkeyedItem : unkeyedItems) {
            unkeyedItem.setPaymentRequest(this);
            this.items.add(unkeyedItem);
        }
    }

    public void update(String purpose, BigDecimal amount, List<PaymentRequestItem> items) {
        if (this.status != PaymentRequestStatus.DRAFT) {
            throw new IllegalStateException("Only draft payment requests can be edited.");
        }
        this.purpose = purpose;
        this.amount = amount;
        replaceItems(items);
        recalculateAmount();
    }

    public void recalculateAmount() {
        if (!items.isEmpty()) {
            this.amount = items.stream()
                    .map(PaymentRequestItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public void submit() {
        if (this.status != PaymentRequestStatus.DRAFT) {
            throw new IllegalStateException("Only draft payment requests can be submitted.");
        }
        if (this.amount == null || this.amount.signum() <= 0) {
            throw new IllegalStateException("A payment request must have a positive amount.");
        }
        this.status = PaymentRequestStatus.PENDING;
        this.submittedAt = OffsetDateTime.now();
    }

    public void decide(PaymentRequestStatus decision, String reason, User approver) {
        this.status = decision;
        this.decisionReason = reason;
        this.decidedBy = approver;
        this.decidedAt = OffsetDateTime.now();
    }
}