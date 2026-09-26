package com.loft.hotel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund")
public class Refund {
    @Id
    @Column(name = "refund_id", unique = true, nullable = false)
    private String refundId;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    public enum RefundType{
        Guest_Initiated, Admin_Initiated
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private RefundType refundType;

    public enum RefundStatus{
        PENDING, APPROVED, REFUNDED, REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus;

    @Column(name = "refund_reason", nullable = false)
    private String refundReason;

    @CreationTimestamp
    @Column(name = "refund_datetime", nullable = false)
    private LocalDateTime refundDateTime;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    // No-argument constructor
    public Refund() {
    }

    // Parameterized constructor
    public Refund( String refundId, String paymentId, RefundType refundType, RefundStatus refundStatus, String refundReason, BigDecimal refundAmount ) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.refundType = refundType;
        this.refundStatus = refundStatus;
        this.refundReason = refundReason;
        this.refundAmount = refundAmount;
    }

    public void setRefundId(String refundId){ this.refundId = refundId; }
    public String getRefundId(){ return refundId; }

    public void setPaymentId(String paymentId){ this.paymentId = paymentId; }
    public String getPaymentId(){ return paymentId; }

    public void setRefundType(RefundType refundType){ this.refundType = refundType; }
    public RefundType getRefundType(){ return refundType; }

    public void setRefundStatus(RefundStatus refundStatus){ this.refundStatus = refundStatus; }
    public RefundStatus getRefundStatus(){ return refundStatus; }

    public void setRefundReason(String refundReason){ this.refundReason = refundReason; }
    public String getRefundReason(){ return refundReason; }

    public void setRefundDateTime(LocalDateTime refundDateTime){ this.refundDateTime = refundDateTime; }
    public LocalDateTime getRefundDateTime(){ return refundDateTime; }

    public void setRefundAmount(BigDecimal refundAmount){ this.refundAmount = refundAmount; }
    public BigDecimal getRefundAmount(){ return refundAmount; }
}