package com.loft.hotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


// Guest details now live in the Guest entity, referenced here by guest_id.
// The actual rooms booked live in ReservationRoomSelection, since one
// reservation can cover more than one room.
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_guest"))
    private Guest guest;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate = LocalDateTime.now();

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // Stored as text in the database ("PENDING", "CONFIRMED", "CANCELLED"),
    // but handled as the ReservationStatus enum everywhere in the code.
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status")
    private ReservationStatus reservationStatus = ReservationStatus.PENDING;

    public Reservation() {
    }

    public Integer getReservationId() {
        return reservationId; }
    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId; }

    public Guest getGuest() {
        return guest; }
    public void setGuest(Guest guest) {
        this.guest = guest; }

    public LocalDate getCheckInDate() {
        return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() {
        return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate; }

    public LocalDateTime getBookingDate() {
        return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate; }

    public Integer getNumberOfGuests() {
        return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests; }

    public BigDecimal getTotalAmount() {
        return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount; }

    public ReservationStatus getReservationStatus() {
        return reservationStatus; }
    // Note: no legality checking here (e.g. this would allow CANCELLED -> PENDING).
    // That rule-checking belongs in ReservationService.confirm() / .cancel(), not here.
    public void setReservationStatus(ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus; }
}