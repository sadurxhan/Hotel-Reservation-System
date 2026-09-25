
package com.loft.hotel.service;

import com.loft.hotel.entity.Reservation;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    // Check whether the requested dates are available
    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {

        if (checkIn == null || checkOut == null) {
            return false;
        }

        if (!checkOut.isAfter(checkIn)) {
            return false;
        }

        return !repository.hasOverlap(
                new ReservationRepository.LocalDateRange(
                        checkIn,
                        checkOut
                )
        );
    }

    // Create a new reservation
    // The reservation starts with PENDING status
    public synchronized Reservation createReservation(Reservation reservation) {

        // Validate guest name
        if (reservation.getGuestName() == null
                || reservation.getGuestName().isBlank()) {

            throw new IllegalArgumentException("Guest name is required.");
        }

        // Validate email
        if (reservation.getGuestEmail() == null
                || reservation.getGuestEmail().isBlank()) {

            throw new IllegalArgumentException("Guest email is required.");
        }

        // Validate room type
        if (reservation.getRoomType() == null
                || reservation.getRoomType().isBlank()) {

            throw new IllegalArgumentException("Room type is required.");
        }

        // Validate check-in and check-out dates
        if (reservation.getCheckInDate() == null
                || reservation.getCheckOutDate() == null) {

            throw new IllegalArgumentException("Please choose both check-in and check-out dates.");
        }

        // Check-in date cannot be in the past
        if (reservation.getCheckInDate().isBefore(LocalDate.now())) {

            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }

        // Check-out date must be after check-in date
        if (!reservation.getCheckOutDate()
                .isAfter(reservation.getCheckInDate())) {

            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        // Check whether the selected dates are already booked
        if (!isAvailable(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate())) {

            throw new IllegalArgumentException("Those dates are already booked. Please choose different dates.");
        }

        // New reservations start with PENDING status
        reservation.setStatus("PENDING");

        return repository.save(reservation);
    }

    // Get a reservation by its ID
    public Reservation getById(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Reservation " + id + " not found"));
    }

    // Get all reservations
    public List<Reservation> getAll() {
        return repository.findAll();
    }

    // Confirm a PENDING reservation
    // This can be called after successful payment
    public Reservation confirm(Long id) {

        Reservation reservation = getById(id);

        if (!"PENDING".equals(reservation.getStatus())) {

            throw new IllegalStateException("Only PENDING reservations can be confirmed.");
        }

        // Change the status to CONFIRMED
        reservation.setStatus("CONFIRMED");

        return repository.save(reservation);
    }

    // Cancel a reservation
    public Reservation cancel(Long id) {

        Reservation reservation = getById(id);

        if ("CANCELLED".equals(reservation.getStatus())) {

            throw new IllegalStateException("Reservation is already cancelled.");
        }

        // Change the status to CANCELLED
        reservation.setStatus("CANCELLED");

        return repository.save(reservation);
    }
}

        }