package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    // Spring injects the repositories
    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository, ReservationRepository reservationRepository){
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    // Create a refund request
    @Transactional
    public Refund createRefund(String paymentId, Refund.RefundType refundType, String refundReason){
        // Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found: " + paymentId));

        // Only PAID payments can be refunded
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Only PAID payments can be refunded."
            );
        }

        // Find the reservation
        Reservation reservation = reservationRepository
                .findById(payment.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + payment.getReservationId()
                        ));

        // Get the number of days before check-in
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(
                LocalDate.now(),
                reservation.getCheckInDate()
        );

        // Calculate refund percentage
        BigDecimal refundPercentage;

        if (daysBeforeCheckIn >= 5) {
            // 5 or more days before check-in = 100%
            refundPercentage = new BigDecimal("1.00");

        } else if (daysBeforeCheckIn >= 3) {
            // 3-4 days before check-in = 75%
            refundPercentage = new BigDecimal("0.75");

        } else {
            // 0-2 days before check-in = no refund
            refundPercentage = BigDecimal.ZERO;
        }

        // Calculate refund amount
        BigDecimal refundAmount = payment.getAmount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Generate refund ID
        String refundId = UUID.randomUUID().toString();

        // Create refund
        Refund refund = new Refund(
                refundId,
                paymentId,
                refundType,
                Refund.RefundStatus.PENDING,
                refundReason,
                refundAmount
        );

        // Save refund request
        return refundRepository.save(refund);
    }

    // Find refund by ID
    public Refund getRefund(String refundId) {

        return refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Refund not found: " + refundId
                        ));
    }

    // Approve a refund
    @Transactional
    public Refund approveRefund(String refundId) {

        Refund refund = getRefund(refundId);

        // Only pending refunds can be approved
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING refunds can be approved."
            );
        }

        // No refund is available
        if (refund.getRefundAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    "This cancellation is not eligible for a refund."
            );
        }

        refund.setRefundStatus(Refund.RefundStatus.APPROVED);

        return refundRepository.save(refund);
    }

    // Mark refund as completed
    @Transactional
    public Refund markRefundAsRefunded(String refundId) {

        Refund refund = getRefund(refundId);

        // Only approved refunds can be completed
        if (refund.getRefundStatus() != Refund.RefundStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED refunds can be marked as REFUNDED."
            );
        }

        refund.setRefundStatus(Refund.RefundStatus.REFUNDED);

        return refundRepository.save(refund);
    }

    // Reject a refund
    @Transactional
    public Refund rejectRefund(String refundId) {

        Refund refund = getRefund(refundId);

        // Only pending refunds can be rejected
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING refunds can be rejected."
            );
        }

        refund.setRefundStatus(Refund.RefundStatus.REJECTED);

        return refundRepository.save(refund);
    }
}