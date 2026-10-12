package com.loft.hotel.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class StaySelectionDTO {

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    //List to hold the selected room IDs ([1], [2], or [1, 2] if booking both)
    private List<Integer> selectedRoomIds = new ArrayList<>();
    private long numberOfNights;

    public StaySelectionDTO() {
    }

    public StaySelectionDTO(LocalDate checkInDate, LocalDate checkOutDate, Integer guestCount) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestCount = guestCount;
        if (checkInDate != null && checkOutDate != null) {
            this.numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        }
    }

    //Max capacity is 5 guests per room
    //If party size exceeds 5, both rooms must be booked
    public boolean requiresBothRooms() {
        return this.guestCount != null && this.guestCount > 5;
    }

    //Getters and Setters
    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
        if (this.checkInDate != null && this.checkOutDate != null) {
            this.numberOfNights = ChronoUnit.DAYS.between(this.checkInDate, this.checkOutDate);
        }
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
        if (this.checkInDate != null && this.checkOutDate != null) {
            this.numberOfNights = ChronoUnit.DAYS.between(this.checkInDate, this.checkOutDate);
        }
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public List<Integer> getSelectedRoomIds() {
        return selectedRoomIds;
    }

    public void setSelectedRoomIds(List<Integer> selectedRoomIds) {
        this.selectedRoomIds = selectedRoomIds;
    }

    public long getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(long numberOfNights) {
        this.numberOfNights = numberOfNights;
    }
}