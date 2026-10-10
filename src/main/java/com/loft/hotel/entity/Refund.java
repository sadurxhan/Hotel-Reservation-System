
package com.loft.hotel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "refund",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_refund_payment",
                        columnNames = "payment_id"
                )
        }
)
public class Refund {

    @Id
    @Column(name = "refund_id", length = 40)
    private String refundId;

    @Column(name = "payment_id", nullable = false, length = 40)
    private String paymentId;

    public enum RefundStatus {
        PENDING, APPROVED, REFUNDED, REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus;

    @Column(name = "refund_reason", nullable = false, length = 300)
    private String refundReason;

    @CreationTimestamp
    @Column(name = "refund_datetime", nullable = false)
    private LocalDateTime refundDateTime;

    @Column(name = "refund_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    public Refund() {
    }

    public Refund(String refundId, String paymentId, RefundStatus refundStatus, String refundReason, BigDecimal refundAmount) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.refundStatus = refundStatus;
        this.refundReason = refundReason;
        this.refundAmount = refundAmount;
    }

    public void setRefundId(String refundId){ this.refundId = refundId; }
    public String getRefundId(){ return refundId; }

    public void setPaymentId(String paymentId){ this.paymentId = paymentId; }
    public String getPaymentId(){ return paymentId; }

    public void setRefundStatus(RefundStatus refundStatus){ this.refundStatus = refundStatus; }
    public RefundStatus getRefundStatus(){ return refundStatus; }

    public void setRefundReason(String refundReason){ this.refundReason = refundReason; }
    public String getRefundReason(){ return refundReason; }

    public void setRefundDateTime(LocalDateTime refundDateTime){ this.refundDateTime = refundDateTime; }
    public LocalDateTime getRefundDateTime(){ return refundDateTime; }

    public void setRefundAmount(BigDecimal refundAmount){this.refundAmount = refundAmount; }
    public BigDecimal getRefundAmount(){ return refundAmount; }
}