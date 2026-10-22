package com.ntdhtcct.domain.payments;

import com.ntdhtcct.domain.acceptance.AcceptanceItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_request_items")
public class PaymentRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequest paymentRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acceptance_item_id", nullable = false)
    private AcceptanceItem acceptanceItem;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected PaymentRequestItem() {
    }

    public PaymentRequestItem(
            AcceptanceItem acceptanceItem,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount) {
        this.acceptanceItem = acceptanceItem;
        this.itemName = acceptanceItem.getItemName();
        this.unit = acceptanceItem.getUnit();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(PaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public AcceptanceItem getAcceptanceItem() {
        return acceptanceItem;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void update(BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, String itemName, String unit) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
        if (itemName != null) this.itemName = itemName;
        if (unit != null) this.unit = unit;
    }
}
