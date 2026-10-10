package com.loft.hotel.controller;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class MockPaymentController {

    private final PaymentService paymentService;

    public MockPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /*
     * Opens the mock payment gateway.
     */
    @GetMapping("/mock-gateway")
    public String mockGateway(
            @RequestParam String paymentId,
            Model model) {

        Payment payment =
                paymentService.getPayment(paymentId);

        if (payment.getPaymentStatus()
                != Payment.PaymentStatus.PENDING) {

            throw new IllegalStateException(
                    "This payment is no longer pending."
            );
        }

        model.addAttribute(
                "payment",
                payment
        );

        return "billing/mock-gateway";
    }

    /*
     * Simulates a successful payment.
     */
    @PostMapping("/api/mock-payment/{paymentId}/pay")
    public String successfulPayment(
            @PathVariable String paymentId,
            @RequestParam String cardNumber,
            @RequestParam String cardHolder,
            @RequestParam String expiryDate,
            @RequestParam String cvv) {

        Payment payment =
                paymentService.getPayment(paymentId);

        /*
         * We do NOT store real card details.
         * This is only a demonstration gateway.
         */

        BigDecimal amount =
                payment.getAmount();

        String transactionId =
                "MOCK-" + UUID.randomUUID();

        paymentService.markPaymentAsPaid(
                paymentId,
                transactionId,
                amount
        );

        return "redirect:/payment/result?paymentId="
                + paymentId;
    }

    /*
     * Simulates a failed payment.
     */
    @PostMapping("/api/mock-payment/{paymentId}/fail")
    public String failedPayment(
            @PathVariable String paymentId) {

        paymentService.markPaymentAsFailed(
                paymentId
        );

        return "redirect:/payment/result?paymentId="
                + paymentId;
    }
}