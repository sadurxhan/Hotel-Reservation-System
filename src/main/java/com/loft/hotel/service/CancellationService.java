package com.loft.hotel.service;

import com.loft.hotel.entity.Cancellation;
import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationStatus;
import com.loft.hotel.repository.CancellationRepository;
import com.loft.hotel.repository.PaymentRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CancellationService {

    private final CancellationRepository cancellationRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public CancellationService(
            CancellationRepository cancellationRepository,
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository) {

        this.cancellationRepository = cancellationRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    /*
     * Create cancellation request.
     */
    @Transactional
    public Cancellation createCancellation(
            Integer reservationId,
            Cancellation.RequestedBy requestedBy,
            String cancellationReason) {

        if (reservationId == null) {
            throw new IllegalArgumentException(
                    "Reservation ID is required."
            );
        }

        if (requestedBy == null) {
            throw new IllegalArgumentException(
                    "Requested by is required."
            );
        }

        if (cancellationReason == null
                || cancellationReason.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Cancellation reason is required."
            );
        }

        /*
         * ReservationRepository cannot be modified,
         * so use the existing findById().
         */
        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + reservationId
                                ));

        /*
         * Only active reservations can be cancelled.
         */
        if (reservation.getReservationStatus()
                != ReservationStatus.PENDING
                && reservation.getReservationStatus()
                != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "This reservation cannot be cancelled."
            );
        }

        /*
         * Check whether a cancellation already exists.
         *
         * REQUESTED -> cannot create another request
         * APPROVED  -> reservation already cancelled
         * REJECTED  -> allow a new cancellation request
         */
        var existingCancellation =
                cancellationRepository.findByReservationId(
                        reservationId
                );

        if (existingCancellation.isPresent()) {

            Cancellation existing =
                    existingCancellation.get();

            if (existing.getCancellationStatus()
                    == Cancellation.CancellationStatus.REQUESTED) {

                throw new IllegalStateException(
                        "A cancellation request is already pending."
                );
            }

            if (existing.getCancellationStatus()
                    == Cancellation.CancellationStatus.APPROVED) {

                throw new IllegalStateException(
                        "This reservation has already been cancelled."
                );
            }

            /*
             * Because cancellation.reservation_id is UNIQUE,
             * remove the previous rejected record before creating
             * a new cancellation request.
             */
            if (existing.getCancellationStatus()
                    == Cancellation.CancellationStatus.REJECTED) {

                cancellationRepository.delete(existing);
                cancellationRepository.flush();
            }
        }

        String cancellationId =
                UUID.randomUUID().toString();

        Cancellation cancellation =
                new Cancellation(
                        cancellationId,
                        reservation.getReservationId(),
                        requestedBy,
                        cancellationReason.trim(),
                        Cancellation.CancellationStatus.REQUESTED
                );

        return cancellationRepository.save(cancellation);
    }

    /*
     * Find cancellation.
     */
    public Cancellation getCancellation(
            String cancellationId) {

        if (cancellationId == null
                || cancellationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Cancellation ID is required."
            );
        }

        return cancellationRepository.findById(cancellationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cancellation not found: "
                                        + cancellationId
                        ));
    }

    /*
     * Approve cancellation.
     */
    @Transactional
    public Cancellation approveCancellation(
            String cancellationId) {

        Cancellation cancellation =
                getCancellation(cancellationId);

        if (cancellation.getCancellationStatus()
                != Cancellation.CancellationStatus.REQUESTED) {

            throw new IllegalStateException(
                    "Only REQUESTED cancellations can be approved."
            );
        }

        /*
         * ReservationRepository cannot be modified,
         * so use the existing findById().
         */
        Reservation reservation =
                reservationRepository.findById(
                                cancellation.getReservationId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + cancellation.getReservationId()
                                ));

        /*
         * The reservation must still be active.
         */
        if (reservation.getReservationStatus()
                != ReservationStatus.PENDING
                && reservation.getReservationStatus()
                != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "This reservation can no longer be cancelled."
            );
        }

        /*
         * If there is a PENDING payment, invalidate it.
         *
         * Otherwise the old payment could later become PAID
         * after the reservation has already been cancelled.
         */
        var pendingPayment =
                paymentRepository
                        .findByReservationIdAndPaymentStatus(
                                reservation.getReservationId(),
                                Payment.PaymentStatus.PENDING
                        );

        if (pendingPayment.isPresent()) {

            Payment payment = pendingPayment.get();

            payment.setPaymentStatus(
                    Payment.PaymentStatus.FAILED
            );

            paymentRepository.save(payment);
        }

        /*
         * If the reservation was CONFIRMED, its PAID payment
         * remains PAID.
         *
         * RefundService handles the refund separately.
         */

        cancellation.setCancellationStatus(
                Cancellation.CancellationStatus.APPROVED
        );

        reservation.setReservationStatus(
                ReservationStatus.CANCELLED
        );

        reservationRepository.save(reservation);

        return cancellationRepository.save(cancellation);
    }

    /*
     * Reject cancellation.
     *
     * The reservation remains unchanged.
     */
    @Transactional
    public Cancellation rejectCancellation(
            String cancellationId) {

        Cancellation cancellation =
                getCancellation(cancellationId);

        if (cancellation.getCancellationStatus()
                != Cancellation.CancellationStatus.REQUESTED) {

            throw new IllegalStateException(
                    "Only REQUESTED cancellations can be rejected."
            );
        }

        cancellation.setCancellationStatus(
                Cancellation.CancellationStatus.REJECTED
        );

        return cancellationRepository.save(cancellation);
    }
}