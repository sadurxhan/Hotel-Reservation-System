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

    public PaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            InvoiceService invoiceService) {

        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.invoiceService = invoiceService;
    }

    /*
     * Create a new payment.
     *
     * If a PENDING payment already exists for the reservation,
     * reuse it instead of creating another payment.
     */
    @Transactional
    public Payment createPayment(
            Integer reservationId,
            Payment.PaymentMethod paymentMethod) {

        // Find the reservation
        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + reservationId
                                ));

        /*
         * Check whether this reservation already has
         * a PENDING payment.
         */
        Optional<Payment> existingPayment =
                paymentRepository
                        .findByReservationIdAndPaymentStatus(
                                reservationId,
                                Payment.PaymentStatus.PENDING
                        );

        /*
         * Reuse the existing PENDING payment.
         */
        if (existingPayment.isPresent()) {

            Payment payment = existingPayment.get();

            // Update selected payment method
            payment.setPaymentMethod(paymentMethod);

            /*
             * Keep the payment amount synchronized
             * with the current reservation amount.
             */
            payment.setAmount(
                    reservation.getTotalAmount()
            );

            return paymentRepository.save(payment);
        }

        /*
         * No PENDING payment exists,
         * so create a new payment.
         */
        Payment payment = new Payment();

        payment.setPaymentId(
                UUID.randomUUID().toString()
        );

        payment.setReservationId(
                reservationId
        );

        payment.setAmount(
                reservation.getTotalAmount()
        );

        payment.setPaymentDateTime(
                LocalDateTime.now()
        );

        payment.setPaymentMethod(
                paymentMethod
        );

        payment.setPaymentStatus(
                Payment.PaymentStatus.PENDING
        );

        payment.setTransactionId(null);

        return paymentRepository.save(payment);
    }

    /*
     * Get a payment by payment ID.
     */
    public Payment getPayment(String paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: "
                                        + paymentId
                        ));
    }

    /*
     * Mark payment as PAID after successful
     * payment verification.
     */
    @Transactional
    public Payment markPaymentAsPaid(
            String paymentId,
            String transactionId,
            BigDecimal paidAmount) {

        // Find the payment
        Payment payment =
                getPayment(paymentId);

        /*
         * Idempotency:
         *
         * If the payment is already PAID,
         * simply return it.
         *
         * This prevents duplicate callbacks from
         * creating another invoice.
         */
        if (payment.getPaymentStatus()
                == Payment.PaymentStatus.PAID) {

            return payment;
        }

        /*
         * Verify that the amount received from
         * the payment gateway matches the amount
         * expected by our system.
         */
        if (payment.getAmount()
                .compareTo(paidAmount) != 0) {

            throw new IllegalStateException(
                    "Amount mismatch for payment "
                            + paymentId
            );
        }

        /*
         * Update payment details.
         */
        payment.setPaymentStatus(
                Payment.PaymentStatus.PAID
        );

        payment.setTransactionId(
                transactionId
        );

        payment.setPaymentDateTime(
                LocalDateTime.now()
        );

        paymentRepository.save(payment);

        /*
         * Find the reservation connected
         * to this payment.
         */
        Reservation reservation =
                reservationRepository.findById(
                        payment.getReservationId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + payment.getReservationId()
                        ));

        /*
         * Successful payment automatically
         * confirms the reservation.
         */
        reservation.setReservationStatus(
                ReservationStatus.CONFIRMED
        );

        reservationRepository.save(reservation);

        /*
         * Automatically create the invoice.
         *
         * InvoiceService is responsible for
         * invoice creation.
         */
        invoiceService.createInvoice(
                payment.getPaymentId()
        );

        return payment;
    }

    /*
     * Mark payment as FAILED.
     */
    @Transactional
    public Payment markPaymentAsFailed(
            String paymentId) {

        Payment payment =
                getPayment(paymentId);

        /*
         * Never change a successful payment
         * from PAID back to FAILED.
         */
        if (payment.getPaymentStatus()
                == Payment.PaymentStatus.PAID) {

            return payment;
        }

        payment.setPaymentStatus(
                Payment.PaymentStatus.FAILED
        );

        return paymentRepository.save(payment);
    }
}