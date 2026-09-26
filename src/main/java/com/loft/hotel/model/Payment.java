package com.loft.hotel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @Column(name = "payment_id", length = 40)
    private String paymentId;

    @Column(name = "reservation_id", nullable = false)
    private int reservationId;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "payment_datetime", nullable = false)
    private LocalDateTime paymentDateTime;

    public enum PaymentMethod {
        Credit_Card, Debit_Card, Online_Banking, QR_Payment
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    public enum PaymentStatus{
        PENDING, PAID, FAILED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "transaction_id", unique = true, length = 40)
    private String transactionId;

    public Payment(){

    }

    public Payment(String paymentId, int reservationId, BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus paymentStatus, String transactionId){
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
    }
    public void setPaymentId(String paymentId){ this.paymentId = paymentId; }
    public String getPaymentId(){ return paymentId; }

    public void setReservationId(int reservationId){ this.reservationId = reservationId; }
    public int getReservationId(){ return reservationId; }

    public void setAmount(BigDecimal amount){ this.amount = amount; }
    public BigDecimal getAmount(){ return amount; }

    public void setPaymentMethod(PaymentMethod paymentMethod){ this.paymentMethod = paymentMethod; }
    public PaymentMethod getPaymentMethod(){ return paymentMethod; }

    public void setPaymentStatus(PaymentStatus paymentStatus){ this.paymentStatus = paymentStatus; }
    public PaymentStatus getPaymentStatus(){ return paymentStatus; }

    public void setTransactionId(String transactionId){ this.transactionId = transactionId; }
    public String getTransactionId(){ return transactionId; }
}