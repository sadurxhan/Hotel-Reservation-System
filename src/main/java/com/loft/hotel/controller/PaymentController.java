package com.loft.hotel.controller;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create a new PENDING payment
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestParam int reservationId, @RequestParam Payment.PaymentMethod paymentMethod) {
        Payment payment = paymentService.createPayment(reservationId, paymentMethod);
        return ResponseEntity.ok(payment);
    }

    // Get payment by ID
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable String paymentId) {

        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    // Mark payment as PAID
    // Temporary endpoint for testing before PayHere integration
    @PostMapping("/{paymentId}/paid")
    public ResponseEntity<Payment> markPaymentAsPaid(
            @PathVariable String paymentId,
            @RequestParam String transactionId,
            @RequestParam BigDecimal paidAmount) {

        Payment payment = paymentService.markPaymentAsPaid(paymentId, transactionId, paidAmount);
        return ResponseEntity.ok(payment);
    }

    // Mark payment as FAILED
    @PostMapping("/{paymentId}/failed")
    public ResponseEntity<Payment> markPaymentAsFailed(@PathVariable String paymentId) {
        Payment payment = paymentService.markPaymentAsFailed(paymentId);
        return ResponseEntity.ok(payment);
    }
}