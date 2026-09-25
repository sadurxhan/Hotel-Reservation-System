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

    public ReservationController(
            ReservationService reservationService) {

        this.reservationService = reservationService;
    }

    /**
     * Creates a new reservation.
     *
     * POST /api/reservations
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody Reservation reservation) {

        try {

            Reservation created =
                    reservationService.createReservation(reservation);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "An unexpected error occurred while processing your booking."
                    );
        }
    }

    /**
     * Gets all reservations.
     *
     * GET /api/reservations
     */
    @GetMapping
    public ResponseEntity<?> getAllReservations() {

        return ResponseEntity.ok(
                reservationService.getAll()
        );
    }

    /**
     * Gets one reservation by ID.
     *
     * GET /api/reservations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(
            @PathVariable Long id) {

        try {

            Reservation reservation =
                    reservationService.getById(id);

            return ResponseEntity.ok(reservation);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    /**
     * Checks whether dates are available.
     *
     * Example:
     * GET /api/reservations/availability
     * ?checkIn=2026-10-10&checkOut=2026-10-12
     */
    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut) {

        boolean available =
                reservationService.isAvailable(
                        checkIn,
                        checkOut
                );

        return ResponseEntity.ok(available);
    }

    /**
     * Confirms a PENDING reservation.
     *
     * PUT /api/reservations/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(
            @PathVariable Long id) {

        try {

            Reservation confirmed =
                    reservationService.confirm(id);

            return ResponseEntity.ok(confirmed);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * Cancels a reservation.
     *
     * PUT /api/reservations/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long id) {

        try {

            Reservation cancelled =
                    reservationService.cancel(id);

            return ResponseEntity.ok(cancelled);

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}