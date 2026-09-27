package com.loft.hotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Links a Reservation to the specific room(s) booked within it.
// One reservation can have several rows here if more than one room is booked together.
@Entity
@Table(name = "reservation_room_selection")
public class ReservationRoomSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "selection_id")
    private Integer selectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_selection_reservation"))
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_selection_room"))
    private Room room;

    @Column(name = "no_of_guests", nullable = false)
    private Integer noOfGuests;

    // Price is copied from the room's tier at the time of booking, so a later
    // price change doesn't alter what a guest already agreed to pay.
    @Column(name = "room_price_per_night", nullable = false)
    private BigDecimal roomPricePerNight;

    public ReservationRoomSelection() {
    }

    public Integer getSelectionId() {
        return selectionId; }
    public void setSelectionId(Integer selectionId) {
        this.selectionId = selectionId; }

    public Reservation getReservation() {
        return reservation; }
    public void setReservation(Reservation reservation) {
        this.reservation = reservation; }

    public Room getRoom() {
        return room; }
    public void setRoom(Room room) {
        this.room = room; }

    public Integer getNoOfGuests() {
        return noOfGuests; }
    public void setNoOfGuests(Integer noOfGuests) {
        this.noOfGuests = noOfGuests; }

    public BigDecimal getRoomPricePerNight() {
        return roomPricePerNight; }
    public void setRoomPricePerNight(BigDecimal roomPricePerNight) {
        this.roomPricePerNight = roomPricePerNight; }
}
