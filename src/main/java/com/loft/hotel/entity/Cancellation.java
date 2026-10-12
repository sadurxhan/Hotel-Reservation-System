package com.loft.hotel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation")
public class Cancellation {
    @Id
    @Column(name = "cancellation_id", length = 40)
    private String cancellationId;

    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    @CreationTimestamp
    @Column(name = "requested_datetime", nullable = false)
    private LocalDateTime requestedDateTime;

    public enum RequestedBy {
        Guest, Admin
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false)
    private RequestedBy requestedBy;

    @Column(name = "cancellation_reason", nullable = false, length = 300)
    private String cancellationReason;

    public enum CancellationStatus {
        REQUESTED, APPROVED, REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_status", nullable = false)
    private CancellationStatus cancellationStatus;

    public Cancellation() {
    }

    public Cancellation(String cancellationId, Integer reservationId, RequestedBy requestedBy, String cancellationReason, CancellationStatus cancellationStatus) {
        this.cancellationId = cancellationId;
        this.reservationId = reservationId;
        this.requestedBy = requestedBy;
        this.cancellationReason = cancellationReason;
        this.cancellationStatus = cancellationStatus;
    }

    public String getCancellationId(){ return cancellationId; }
    public void setCancellationId(String cancellationId){ this.cancellationId = cancellationId; }

    public Integer getReservationId(){ return reservationId; }
    public void setReservationId(Integer reservationId){ this.reservationId = reservationId; }

    public LocalDateTime getRequestedDateTime(){ return requestedDateTime; }
    public void setRequestedDateTime(LocalDateTime requestedDateTime){ this.requestedDateTime = requestedDateTime; }

    public RequestedBy getRequestedBy(){ return requestedBy; }
    public void setRequestedBy(RequestedBy requestedBy){ this.requestedBy = requestedBy; }

    public String getCancellationReason(){ return cancellationReason; }
    public void setCancellationReason(String cancellationReason){ this.cancellationReason = cancellationReason; }

    public CancellationStatus getCancellationStatus(){ return cancellationStatus; }
    public void setCancellationStatus(CancellationStatus cancellationStatus){ this.cancellationStatus = cancellationStatus; }
}