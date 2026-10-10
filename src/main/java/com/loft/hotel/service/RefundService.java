package com.loft.hotel.service;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Refund;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.exception.ResourceNotFoundException;
import com.loft.hotel.repository.PaymentRepository;
import com.loft.hotel.repository.RefundRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MailService mailService;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         ReservationRepository reservationRepository,
                         MailService mailService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.mailService = mailService;
    }

    // Called by CancellationService after a cancellation is approved.
    // Policy: 5+ days = 100%, 3-4 days = 75%, 0-2 days or past check-in = no refund (no record created).
    // Returns empty if no refund is applicable.
    @Transactional
    public Optional<Refund> createRefundIfApplicable(Reservation reservation, LocalDate requestDate) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }
        if (requestDate == null) {
            throw new IllegalArgumentException("Cancellation request date cannot be null.");
        }
        if (reservation.getCheckInDate() == null) {
            throw new IllegalStateException("Reservation check-in date is missing.");
        }

        Optional<Payment> paid = paymentRepository.findByReservationIdAndPaymentStatus(
                reservation.getReservationId(), Payment.PaymentStatus.PAID);
        if (paid.isEmpty()) return Optional.empty(); // nothing was paid

        Payment payment = paid.get();
        if (refundRepository.findByPaymentId(payment.getPaymentId()).isPresent()) {
            throw new IllegalStateException("A refund already exists for this payment.");
        }

        // Negative days (past check-in) fall through to 0% and create no refund record.
        long days = ChronoUnit.DAYS.between(requestDate, reservation.getCheckInDate());
        int percent = days >= 5 ? 100 : (days >= 3 ? 75 : 0);
        if (percent == 0) return Optional.empty();

        BigDecimal amount = payment.getAmount()
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Refund refund = new Refund(UUID.randomUUID().toString(), payment.getPaymentId(),
                Refund.RefundStatus.PENDING,
                percent + "% refund per cancellation policy (" + days + " days before check-in).",
                amount);
        return Optional.of(refundRepository.save(refund));
    }

    public Refund getRefund(String refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
    }

    // So the admin portal can show the refund next to its cancellation
    public Refund getRefundByReservationId(Integer reservationId) {
        Payment payment = paymentRepository
                .findByReservationIdAndPaymentStatus(reservationId, Payment.PaymentStatus.PAID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No PAID payment for reservation: " + reservationId));
        return refundRepository.findByPaymentId(payment.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No refund for reservation: " + reservationId));
    }

    public List<Refund> getPendingRefunds() {
        return refundRepository.findByRefundStatus(Refund.RefundStatus.PENDING);
    }

    // Admin has paid the guest manually, now marks it in the system
    @Transactional
    public Refund markRefundAsPaid(String refundId) {
        Refund refund = getRefund(refundId);
        if (refund.getRefundStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException("Only PENDING refunds can be marked as paid.");
        }

        // load everything first, change status last
        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + refund.getPaymentId()));
        Reservation reservation = reservationRepository.findById(payment.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + payment.getReservationId()));

        refund.setRefundStatus(Refund.RefundStatus.REFUNDED);
        Refund saved = refundRepository.save(refund);

        // MailService never throws; a failed email must not undo the recorded refund.
        boolean sent = mailService.send(reservation.getGuest().getEmail(), "Refund paid",
                "Your refund of LKR " + refund.getRefundAmount() + " for reservation #"
                        + reservation.getReservationId() + " has been paid.");
        if (!sent) {
            log.warn("Refund {} recorded as REFUNDED, but the confirmation email was not sent.", refundId);
        }
        return saved;
    }
}