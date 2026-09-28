package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.PaymentRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    // Spring injects PaymentRepository here
    public PaymentService(PaymentRepository paymentRepository, ReservationRepository reservationRepository){
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    // Create a new payment
    public Payment createPayment(Integer reservationId, Payment.PaymentMethod paymentMethod){
        // Generate a unique payment ID
        String paymentId = UUID.randomUUID().toString();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found: " + reservationId));

        BigDecimal amount = reservation.getTotalAmount();

        // Create payment object
        Payment payment = new Payment( paymentId, reservationId, amount, paymentMethod, Payment.PaymentStatus.PENDING, null);

        // Set the payment date/time
        payment.setPaymentDateTime(LocalDateTime.now());

        // Save payment to database
        return paymentRepository.save(payment);
    }

    // Find a payment by its ID
    public Payment getPayment(String paymentId){
        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found: " + paymentId));
    }

    // Mark payment as PAID after successful verification
    @Transactional
    public Payment markPaymentAsPaid(String paymentId, String transactionId, BigDecimal paidAmount){
        // Find the payment
        Payment payment = getPayment(paymentId);

        // Ignore duplicate successful callbacks
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }

        // Check that gateway amount matches our payment amount
        if (payment.getAmount().compareTo(paidAmount) != 0) {
            throw new IllegalStateException(
                    "Amount mismatch for payment " + paymentId
            );
        }

        // Update payment details
        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaymentDateTime(LocalDateTime.now());

        // Save updated payment
        return paymentRepository.save(payment);
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

        // Update status
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);

        // Save updated payment
        return paymentRepository.save(payment);
    }
}