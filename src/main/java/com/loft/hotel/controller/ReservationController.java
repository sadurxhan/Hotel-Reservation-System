package com.loft.hotel.controller;

import com.loft.hotel.dto.BookingRequest;
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

    // POST /api/reservations
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody BookingRequest request) {
        try {
            Reservation created = reservationService.createReservation(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing your booking.");
        }
    }

    // GET /api/reservations
    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    // GET /api/reservations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(reservationService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // GET /api/reservations/availability?roomId=1&checkIn=2027-01-01&checkOut=2027-01-03
    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability(@RequestParam Integer roomId,
                                               @RequestParam LocalDate checkIn,
                                               @RequestParam LocalDate checkOut) {
        boolean available = reservationService.isRoomAvailable(roomId, checkIn, checkOut);
        return ResponseEntity.ok(available);
    }

    // PUT /api/reservations/{id}/confirm
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(reservationService.confirm(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // PUT /api/reservations/{id}/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(reservationService.cancel(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
