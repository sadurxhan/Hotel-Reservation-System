package com.loft.hotel.service;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationStatus;
import com.loft.hotel.exception.ResourceNotFoundException;
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

    @Transactional
    public Payment createPayment(
            Integer reservationId,
            Payment.PaymentMethod paymentMethod) {

        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID is required.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationId));

        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING reservations can be paid.");
        }

        if (paymentRepository.findByReservationIdAndPaymentStatus(
                reservationId, Payment.PaymentStatus.PAID).isPresent()) {
            throw new IllegalStateException(
                    "This reservation already has a successful payment.");
        }

        Optional<Payment> existingPayment =
                paymentRepository.findByReservationIdAndPaymentStatus(
                        reservationId, Payment.PaymentStatus.PENDING);

        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            payment.setPaymentMethod(paymentMethod);
            payment.setAmount(reservation.getTotalAmount());
            return paymentRepository.save(payment);
        }

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

    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));
    }

    @Transactional
    public Payment markPaymentAsPaid(
            String paymentId,
            String transactionId,
            BigDecimal paidAmount) {

        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalStateException("Transaction ID is required.");
        }

        if (paidAmount == null) {
            throw new IllegalStateException("Paid amount is required.");
        }

        Payment payment = paymentRepository.findForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));

        // Make repeated successful notifications idempotent.
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            boolean sameTransaction =
                    transactionId.equals(payment.getTransactionId());

            boolean sameAmount =
                    payment.getAmount() != null
                            && payment.getAmount().compareTo(paidAmount) == 0;

            if (sameTransaction && sameAmount) {
                return payment;
            }

            throw new IllegalStateException(
                    "Payment is already PAID but the notification details do not match.");
        }

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING payments can be marked as PAID.");
        }

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(paidAmount) != 0) {
            throw new IllegalStateException(
                    "Amount mismatch for payment " + paymentId);
        }

        Optional<Payment> transactionPayment =
                paymentRepository.findByTransactionId(transactionId);

        if (transactionPayment.isPresent()
                && !transactionPayment.get().getPaymentId().equals(paymentId)) {
            throw new IllegalStateException(
                    "This transaction ID has already been used.");
        }

        Reservation reservation = reservationRepository.findById(
                        payment.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + payment.getReservationId()));

        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "This reservation is no longer payable. "
                            + "The verified payment requires manual reconciliation.");
        }

        payment.setPaymentStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaymentDateTime(LocalDateTime.now());
        paymentRepository.save(payment);

        reservation.setReservationStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Create the invoice after successful payment verification.
        // PayHereController sends the invoice email after this method returns.
        invoiceService.createInvoice(payment.getPaymentId());

        return payment;
    }

    public Payment getPaidPaymentByReservationId(Integer reservationId) {
        return paymentRepository.findByReservationIdAndPaymentStatus(
                        reservationId, Payment.PaymentStatus.PAID)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No PAID payment for reservation: " + reservationId));
    }

    @Transactional
    public Payment markPaymentAsFailed(String paymentId) {
        Payment payment = paymentRepository.findForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));

        // Repeated notifications must not change the payment again.
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID
                || payment.getPaymentStatus() == Payment.PaymentStatus.FAILED) {
            return payment;
        }

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING payments can be marked as FAILED.");
        }

        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        return paymentRepository.save(payment);
    }
}