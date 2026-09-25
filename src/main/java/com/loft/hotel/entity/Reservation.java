package com.loft.hotel.entity;

import java.time.LocalDate;

public class Reservation {

    private Long id;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String roomType;
    private String status;

    public Reservation() {
    }

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

    // Get ID
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Get guest name
    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    // Get guest email
    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    // Get guest phone
    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    // Get check-in date
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    // Get check-out date
    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    // Get room type
    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    // Get reservation status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}