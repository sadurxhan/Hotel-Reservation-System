package com.loft.hotel.entity;


// PENDING   -> just created, waiting for payment
// CONFIRMED -> payment received, booking is final
// CANCELLED -> cancelled by the guest or the system; dates become free again
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
