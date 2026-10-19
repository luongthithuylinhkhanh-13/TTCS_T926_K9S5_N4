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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        this.status = PaymentRequestStatus.PENDING;
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

    public void decide(PaymentRequestStatus decision, String reason, User approver) {
        this.status = decision;
        this.decisionReason = reason;
        this.decidedBy = approver;
        this.decidedAt = OffsetDateTime.now();
    }
}