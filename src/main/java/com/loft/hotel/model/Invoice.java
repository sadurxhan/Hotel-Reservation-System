package com.loft.hotel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @Column(name = "invoice_id", length = 40)
    private String invoiceId;

    @Column(name = "payment_id", nullable = false, unique = true, length = 40)
    private String paymentId;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    public enum InvoiceStatus{
        ISSUED, SENT
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false)
    private InvoiceStatus invoiceStatus;

    @CreationTimestamp
    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;

    // No-argument constructor
    public Invoice(){
    }

    // Parameterized constructor
    public Invoice( String invoiceId, String paymentId, BigDecimal totalAmount, InvoiceStatus invoiceStatus){
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
        this.invoiceStatus = invoiceStatus;
    }

    public void setInvoiceId(String invoiceId){ this.invoiceId = invoiceId; }
    public String getInvoiceId(){ return invoiceId; }

    public void setPaymentId(String paymentId){ this.paymentId = paymentId; }
    public String getPaymentId(){ return paymentId; }

    public void setTotalAmount(BigDecimal totalAmount){ this.totalAmount = totalAmount; }
    public BigDecimal getTotalAmount(){ return totalAmount; }

    public void setInvoiceStatus(InvoiceStatus invoiceStatus){ this.invoiceStatus = invoiceStatus; }
    public InvoiceStatus getInvoiceStatus(){ return invoiceStatus; }

    public LocalDateTime getIssuedDate(){ return issuedDate; }
    public void setIssuedDate(LocalDateTime issuedDate){ this.issuedDate = issuedDate; }
}