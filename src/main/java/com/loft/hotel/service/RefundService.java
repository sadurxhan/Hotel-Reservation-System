package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final CancellationRepository cancellationRepository;

    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository, ReservationRepository reservationRepository, CancellationRepository cancellationRepository) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.cancellationRepository = cancellationRepository;
    }

    // Create refund after an approved cancellation
    @Transactional
    public Refund createRefund(String paymentId, String cancellationId) {
        // Find payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: " + paymentId
                        ));

        // Payment must be PAID
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Only PAID payments can be refunded."
            );
        }

        // Find cancellation
        Cancellation cancellation = cancellationRepository
                .findById(cancellationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cancellation not found: " + cancellationId
                        ));

        // Cancellation must be APPROVED
        if (cancellation.getCancellationStatus() != Cancellation.CancellationStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED cancellations can be refunded."
            );
        }

        // Find reservation
        Reservation reservation = reservationRepository
                .findById(cancellation.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + cancellation.getReservationId()
                        ));

        // Make sure payment belongs to the same reservation
        if (payment.getReservationId() != reservation.getReservationId()) {
            throw new IllegalStateException(
                    "Payment does not belong to this reservation."
            );
        }

        // Only one refund is allowed per payment
        if (refundRepository.findByPaymentId(paymentId).isPresent()) {
            throw new IllegalStateException(
                    "A refund already exists for this payment."
            );
        }

        // Calculate number of days between cancellation request
        // and the reservation check-in date
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(
                cancellation.getRequestedDateTime().toLocalDate(),
                reservation.getCheckInDate()
        );

        // Determine refund percentage and status
        BigDecimal refundPercentage;
        Refund.RefundStatus refundStatus;
        String refundReason;

        if (daysBeforeCheckIn >= 5) {
            // 5 or more days = 100% refund
            refundPercentage = new BigDecimal("1.00");
            refundStatus = Refund.RefundStatus.PENDING;

            refundReason =
                    "100% refund applied according to cancellation policy "
                            + "(cancellation requested "
                            + daysBeforeCheckIn
                            + " days before check-in).";

        } else if (daysBeforeCheckIn >= 3) {
            // 3-4 days = 75% refund
            refundPercentage = new BigDecimal("0.75");
            refundStatus = Refund.RefundStatus.PENDING;

            refundReason =
                    "75% refund applied according to cancellation policy "
                            + "(cancellation requested "
                            + daysBeforeCheckIn
                            + " days before check-in).";

        } else {
            // 0-2 days = no refund
            // Negative values are also rejected
            refundPercentage = BigDecimal.ZERO;
            refundStatus = Refund.RefundStatus.REJECTED;

            // Prevent negative days from appearing in the reason
            long displayDays = Math.max(daysBeforeCheckIn, 0);

            refundReason =
                    "No refund applicable according to cancellation policy "
                            + "(cancellation requested "
                            + displayDays
                            + " days before check-in).";
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
                refundStatus,
                refundReason,
                refundAmount
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
        if (refund.getRefundStatus()
                != Refund.RefundStatus.PENDING) {

            throw new IllegalStateException(
                    "Only PENDING refunds can be approved."
            );
        }

        // Approve refund
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

        // Mark refund as completed
        refund.setRefundStatus(Refund.RefundStatus.REFUNDED);
        return refundRepository.save(refund);
    }

    // Reject refund manually
    @Transactional
    public Refund rejectRefund(String refundId) {
        Refund refund = getRefund(refundId);

        // Only PENDING refunds can be rejected manually
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING refunds can be rejected."
            );
        }

        // Reject refund
        refund.setRefundStatus(Refund.RefundStatus.REJECTED);
        return refundRepository.save(refund);
    }
}