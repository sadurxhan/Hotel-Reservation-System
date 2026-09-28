package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CancellationService {
    private final CancellationRepository cancellationRepository;
    private final ReservationRepository reservationRepository;

    // Spring injects both repositories
    public CancellationService(CancellationRepository cancellationRepository, ReservationRepository reservationRepository) {
        this.cancellationRepository = cancellationRepository;
        this.reservationRepository = reservationRepository;
    }

    // Create a new cancellation request
    @Transactional
    public Cancellation createCancellation(Integer reservationId, Cancellation.RequestedBy requestedBy, String cancellationReason) {
        // Check that the reservation exists
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: " + reservationId
                        ));

        // A reservation can only have one cancellation
        if (cancellationRepository.findByReservationId(reservationId).isPresent()) {
            throw new IllegalStateException(
                    "Cancellation already exists for this reservation."
            );
        }

        // Generate cancellation ID
        String cancellationId = UUID.randomUUID().toString();

        // Create cancellation request
        Cancellation cancellation = new Cancellation( cancellationId, reservation.getReservationId(), requestedBy, cancellationReason, Cancellation.CancellationStatus.REQUESTED);

        // Save cancellation
        return cancellationRepository.save(cancellation);
    }

    // Find cancellation by ID
    public Cancellation getCancellation(String cancellationId) {
        return cancellationRepository.findById(cancellationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cancellation not found: " + cancellationId
                        ));
    }

    // Approve cancellation
    @Transactional
    public Cancellation approveCancellation(String cancellationId) {
        Cancellation cancellation = getCancellation(cancellationId);

        // Only REQUESTED cancellations can be approved
        if (cancellation.getCancellationStatus()
                != Cancellation.CancellationStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Only REQUESTED cancellations can be approved."
            );
        }

        // Change cancellation status
        cancellation.setCancellationStatus(
                Cancellation.CancellationStatus.APPROVED
        );

        // Find reservation
        Reservation reservation = reservationRepository
                .findById(cancellation.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found: "
                                        + cancellation.getReservationId()
                        ));

        // Cancel the reservation
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        return cancellationRepository.save(cancellation);
    }

    // Reject cancellation
    @Transactional
    public Cancellation rejectCancellation(String cancellationId) {
        Cancellation cancellation = getCancellation(cancellationId);
        // Only REQUESTED cancellations can be rejected
        if (cancellation.getCancellationStatus()
                != Cancellation.CancellationStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Only REQUESTED cancellations can be rejected."
            );
        }

        // Change status to REJECTED
        cancellation.setCancellationStatus(
                Cancellation.CancellationStatus.REJECTED
        );
        return cancellationRepository.save(cancellation);
    }
}