package com.loft.hotel.controller;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AdminGuard adminGuard;

    public PaymentController(
            PaymentService paymentService,
            AdminGuard adminGuard) {
        this.paymentService = paymentService;
        this.adminGuard = adminGuard;
    }

    // Admin retrieves a payment by ID.
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @RequestHeader("X-Admin-Key") String key,
            @PathVariable String paymentId) {

        adminGuard.check(key);
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    // Admin retrieves the paid payment for a reservation.
    @GetMapping
    public ResponseEntity<Payment> getPaidPaymentByReservation(
            @RequestHeader("X-Admin-Key") String key,
            @RequestParam Integer reservationId) {

        adminGuard.check(key);
        return ResponseEntity.ok(
                paymentService.getPaidPaymentByReservationId(reservationId));
    }
}