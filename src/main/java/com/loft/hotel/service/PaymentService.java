package com.loft.hotel.service;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationStatus;
import com.loft.hotel.repository.PaymentRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentRepository paymentRepository, ReservationRepository reservationRepository, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.invoiceService = invoiceService;
    }

    /*
     * Create a payment for a reservation.
     *
     * Rules:
     * 1. Reservation must exist.
     * 2. Only PENDING reservations can be paid.
     * 3. A reservation cannot have another PAID payment.
     * 4. Existing PENDING payment is reused.
     */
    @Transactional
    public Payment createPayment(Integer reservationId, Payment.PaymentMethod paymentMethod) {
        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + reservationId
                                ));

        /*
         * Only PENDING reservations can start
         * a payment process.
         *
         * This prevents CANCELLED and CONFIRMED
         * reservations from being paid again.
         */
        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING reservations can be paid."
            );
        }

        // Prevent multiple successful payments for the same reservation.
        if (paymentRepository
                .findByReservationIdAndPaymentStatus(
                        reservationId,
                        Payment.PaymentStatus.PAID
                )
                .isPresent()) {

            throw new IllegalStateException(
                    "This reservation already has a successful payment."
            );
        }

        // Check whether a PENDING payment already exists.
        Optional<Payment> existingPayment =
                paymentRepository
                        .findByReservationIdAndPaymentStatus(
                                reservationId,
                                Payment.PaymentStatus.PENDING
                        );


        // Reuse existing PENDING payment
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            payment.setPaymentMethod(paymentMethod);
            // Keep amount synchronized with current reservation total.
            payment.setAmount(reservation.getTotalAmount());
            return paymentRepository.save(payment);
        }

        //Create a new payment.
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setReservationId(reservationId);
        payment.setAmount(reservation.getTotalAmount());
        payment.setPaymentDateTime(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId(null);
        return paymentRepository.save(payment);
    }


     // Get payment by ID.
    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: "
                                        + paymentId
                        ));
    }

     // Mark payment as PAID after successful payment verification.
    @Transactional
    public Payment markPaymentAsPaid(String paymentId, String transactionId, BigDecimal paidAmount) {
        Payment payment = getPayment(paymentId);

        // Idempotency: If the payment was already successfully processed, return it without doing anything again.
        if (payment.getPaymentStatus()  == Payment.PaymentStatus.PAID) {
            return payment;
        }

        // Only PENDING payments can become PAID. FAILED payments cannot directly become PAID. A new payment attempt should create a new PENDING payment.
        if (payment.getPaymentStatus()  != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING payments can be marked as PAID."
            );
        }

         // Validate paid amount.
        if (paidAmount == null) {
            throw new IllegalStateException("Paid amount is required.");
        }

        if (payment.getAmount().compareTo(paidAmount) != 0) {
            throw new IllegalStateException("Amount mismatch for payment " + paymentId);
        }

        // Validate transaction ID.
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalStateException(
                    "Transaction ID is required."
            );
        }

        // Find the reservation BEFORE changing the payment to PAID.

        Reservation reservation = reservationRepository.findById(payment.getReservationId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + payment.getReservationId()
                        ));

        // Only PENDING reservations can be confirmed by payment.
        // This prevents: CANCELLED → CONFIRMED
        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "This reservation is no longer payable."
            );
        }


         // Check whether this transaction ID has already been used.

        // This requires: Optional<Payment> findByTransactionId(String transactionId)
        // If your repository does not have this method yet,add it as shown below.
        Optional<Payment> transactionPayment =
                paymentRepository.findByTransactionId(
                        transactionId
                );

        if (transactionPayment.isPresent()
                && !transactionPayment.get()
                .getPaymentId()
                .equals(paymentId)) {

            throw new IllegalStateException(
                    "This transaction ID has already been used."
            );
        }

        // Mark payment as PAID.
        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaymentDateTime(LocalDateTime.now());
        paymentRepository.save(payment);

        // Successful payment confirms reservation.

        reservation.setReservationStatus(ReservationStatus.CONFIRMED);

        reservationRepository.save(reservation);

        // Automatically create invoice. InvoiceService should return the existing invoice if one already exists.
        invoiceService.createInvoice(payment.getPaymentId());
        return payment;
    }

    public Payment getPaidPaymentByReservationId(Integer reservationId) {
        return paymentRepository
                .findByReservationIdAndPaymentStatus(
                        reservationId,
                        Payment.PaymentStatus.PAID
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "No PAID payment for reservation: "
                                        + reservationId
                        ));
    }

    // Mark payment as FAILED.
    @Transactional
    public Payment markPaymentAsFailed(String paymentId) {
        Payment payment = getPayment(paymentId);

        // Never change PAID → FAILED.
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }

        //Only PENDING payments can become FAILED.
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING payments can be marked as FAILED."
            );
        }

        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        return paymentRepository.save(payment);
    }
}