package com.loft.hotel.controller;

import com.loft.hotel.entity.Reservation;
import com.loft.hotel.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Create a new reservation
    // POST /api/reservations
    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody Reservation reservation) {

        try {
            // Validate and save the reservation
            Reservation created =
                    reservationService.createReservation(reservation);

            // Return the created reservation
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);

        } catch (IllegalArgumentException e) {
            // Return 400 when the booking information is invalid
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {
            // Return 500 when an unexpected error occurs
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "An unexpected error occurred while processing your booking."
                    );
        }
    }

    // Get all reservations
    // GET /api/reservations
    @GetMapping
    public ResponseEntity<?> getAllReservations() {

        return ResponseEntity.ok(
                reservationService.getAll()
        );
    }

    // Get one reservation by ID
    // GET /api/reservations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(
            @PathVariable Long id) {

        try {
            // Find the reservation using its ID
            Reservation reservation =
                    reservationService.getById(id);

            return ResponseEntity.ok(reservation);

        } catch (Exception e) {
            // Return 404 when the reservation is not found
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // Check whether the selected dates are available
    // GET /api/reservations/availability
    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut) {

        // Check availability through the service
        boolean available =
                reservationService.isAvailable(
                        checkIn,
                        checkOut
                );

        return ResponseEntity.ok(available);
    }

    // Confirm a pending reservation
    // PUT /api/reservations/{id}/confirm
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(
            @PathVariable Long id) {

        try {
            // Change the reservation status to CONFIRMED
            Reservation confirmed =
                    reservationService.confirm(id);

            return ResponseEntity.ok(confirmed);

        } catch (Exception e) {
            // Return an error when confirmation fails
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // Cancel a reservation
    // PUT /api/reservations/{id}/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long id) {

        try {
            // Change the reservation status to CANCELLED
            Reservation cancelled =
                    reservationService.cancel(id);

            return ResponseEntity.ok(cancelled);

        } catch (Exception e) {
            // Return an error when cancellation fails
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}

