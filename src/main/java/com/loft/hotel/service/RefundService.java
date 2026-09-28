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
    private final CancellationRepository cancellationRepository;

    // Spring injects all required repositories
    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository, ReservationRepository reservationRepository, CancellationRepository cancellationRepository) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.cancellationRepository = cancellationRepository;
    }

    // Create refund after an approved cancellation
    @Transactional
    public Refund createRefund(String paymentId,
                               String cancellationId) {

        // Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: " + paymentId
                        ));

        // Payment must be PAID before it can be refunded
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Only PAID payments can be refunded."
            );
        }

        // Find the cancellation
        Cancellation cancellation = cancellationRepository
                .findById(cancellationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cancellation not found: " + cancellationId
                        ));

        // Cancellation must be approved
        if (cancellation.getCancellationStatus()
                != Cancellation.CancellationStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED cancellations can be refunded."
            );
        }

        // Find the reservation
        Reservation reservation = reservationRepository
                .findById(cancellation.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + cancellation.getReservationId()
                        ));

        // Check that the payment belongs to this reservation
        if (!payment.getReservationId()
                .equals(reservation.getReservationId())) {
            throw new IllegalStateException(
                    "Payment does not belong to this reservation."
            );
        }

        // Check if a refund already exists for this payment
        if (refundRepository.findByPaymentId(paymentId).isPresent()) {
            throw new IllegalStateException(
                    "A refund already exists for this payment."
            );
        }

        // Calculate days between today and check-in
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(
                LocalDate.now(),
                reservation.getCheckInDate()
        );

        // Calculate refund percentage
        BigDecimal refundPercentage;

        if (daysBeforeCheckIn >= 5) {
            // 5 or more days = 100% refund
            refundPercentage = new BigDecimal("1.00");

        } else if (daysBeforeCheckIn >= 3) {
            // 3-4 days = 75% refund
            refundPercentage = new BigDecimal("0.75");

        } else {
            // 0-2 days = no refund
            refundPercentage = BigDecimal.ZERO;
        }

        // Calculate refund amount
        BigDecimal refundAmount = payment.getAmount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Generate refund ID
        String refundId = UUID.randomUUID().toString();

        // Create refund record
        Refund refund = new Refund(refundId, paymentId, cancellation.getRequestedBy() == Cancellation.RequestedBy.Guest ? Refund.RefundType.Guest_Initiated : Refund.RefundType.Admin_Initiated, Refund.RefundStatus.PENDING, cancellation.getCancellationReason(), refundAmount
        );

        // Save refund
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

    // Approve refund
    @Transactional
    public Refund approveRefund(String refundId) {
        Refund refund = getRefund(refundId);

        // Only PENDING refunds can be approved
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING refunds can be approved."
            );
        }

        // A zero amount means there is no refund
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

        // Only APPROVED refunds can be completed
        if (refund.getRefundStatus() != Refund.RefundStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED refunds can be marked as REFUNDED."
            );
        }

        refund.setRefundStatus(Refund.RefundStatus.REFUNDED);
        return refundRepository.save(refund);
    }

    // Reject refund
    @Transactional
    public Refund rejectRefund(String refundId) {

        Refund refund = getRefund(refundId);

        // Only PENDING refunds can be rejected
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING refunds can be rejected."
            );
        }

        refund.setRefundStatus(Refund.RefundStatus.REJECTED);
        return refundRepository.save(refund);
    }
}