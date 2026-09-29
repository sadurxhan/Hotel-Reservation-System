package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final InvoiceService invoiceService;

    // Spring injects the required repositories here
    public PaymentService(PaymentRepository paymentRepository, ReservationRepository reservationRepository, InvoiceRepository invoiceRepository, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.invoiceService = invoiceService;
    }

    // Create a new payment
    public Payment createPayment(Integer reservationId, Payment.PaymentMethod paymentMethod) {
        // Generate a unique payment ID
        String paymentId = UUID.randomUUID().toString();

        // Find the reservation
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: " + reservationId));

        // Get the payment amount from the reservation
        BigDecimal amount = reservation.getTotalAmount();

        // Create payment as PENDING
        Payment payment = new Payment(paymentId, reservationId, amount, paymentMethod, Payment.PaymentStatus.PENDING, null);

        // Set payment date/time
        payment.setPaymentDateTime(LocalDateTime.now());

        if (paymentRepository.findByReservationIdAndPaymentStatus(reservationId, Payment.PaymentStatus.PENDING).isPresent()) {
            throw new IllegalStateException(
                    "A pending payment already exists for this reservation."
            );
        }

        // Save payment
        return paymentRepository.save(payment);
    }

    // Find a payment by its ID
    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: " + paymentId));
    }

    // Mark payment as PAID after successful verification
    @Transactional
    public Payment markPaymentAsPaid(String paymentId, String transactionId, BigDecimal paidAmount) {
        // Find the payment
        Payment payment = getPayment(paymentId);

        // Ignore duplicate successful callbacks
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }

        // Check that the amount received matches
        // the amount expected by our system
        if (payment.getAmount().compareTo(paidAmount) != 0) {
            throw new IllegalStateException(
                    "Amount mismatch for payment " + paymentId
            );
        }

        // Update payment details
        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaymentDateTime(LocalDateTime.now());

        // Save the successful payment
        paymentRepository.save(payment);

        // Find the reservation
        Reservation reservation = reservationRepository
                .findById(payment.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + payment.getReservationId()));

        // Confirm the reservation
        reservation.setReservationStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Create invoice
        invoiceService.createInvoice(payment.getPaymentId());

        // Return the updated payment
        return payment;
    }

    // Mark payment as FAILED
    @Transactional
    public Payment markPaymentAsFailed(String paymentId) {
        // Find the payment
        Payment payment = getPayment(paymentId);

        // Do not change a payment that is already PAID
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }

        // Update payment status
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);

        // Save updated payment
        return paymentRepository.save(payment);
    }
}