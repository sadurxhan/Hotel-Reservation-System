package com.loft.hotel.entity;

import java.time.LocalDate;


// An instance of this class moves through a lifecycle via its "status" field:
//   PENDING   -> reservation created, waiting for payment
//   CONFIRMED -> payment received, booking is final
//   CANCELLED -> guest or system cancelled the booking; dates become free again
// Stored in-memory by ReservationRepository (no database yet).
public class Reservation {

    private Long id;              // Unique ID, assigned automatically when saved (never set manually)
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String roomType;
    private String status;        // PENDING / CONFIRMED / CANCELLED - see comment above the class

    // Empty constructor required by Spring so it can build this object
    // from incoming JSON (the booking form data) before validation happens.
    public Reservation() {
    }

    // Full constructor - mainly useful for tests or manually creating a reservation in code.
    public Reservation(Long id, String guestName, String guestEmail,
                       String guestPhone, LocalDate checkInDate,
                       LocalDate checkOutDate, String roomType,
                       String status) {

        this.id = id;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestPhone = guestPhone;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomType = roomType;
        this.status = status;
    }

    // --- Getters and setters ---
    // Spring needs these to convert this object to/from JSON automatically.
    // No custom logic belongs here - all validation and business rules live in ReservationService.

    public Long getId() {
        return id;
    }

    // Only ReservationRepository should call this, right after generating a new ID.

    // an existing reservation by guessing its ID.
    public void setId(Long id) {
        this.id = id;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }
}
