package com.loft.hotel.service;

import com.loft.hotel.entity.Cancellation;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationStatus;
import com.loft.hotel.exception.ResourceNotFoundException;
import com.loft.hotel.repository.CancellationRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CancellationService {

    private final CancellationRepository cancellationRepository;
    private final ReservationRepository reservationRepository;
    private final RefundService refundService;
    private final MailService mailService;

    public CancellationService(CancellationRepository cancellationRepository,
                               ReservationRepository reservationRepository,
                               RefundService refundService,
                               MailService mailService) {
        this.cancellationRepository = cancellationRepository;
        this.reservationRepository = reservationRepository;
        this.refundService = refundService;
        this.mailService = mailService;
    }

    // ---------- GUEST: submit request (booking reference + email + reason) ----------
    // Auto-checked by the system:
    //   2 days or less before check-in -> REJECTED immediately, guest emailed, admin never sees it
    //   otherwise                      -> REQUESTED, guest emailed, request goes to admin
    @Transactional
    public Cancellation requestCancellation(Integer reservationId, String email, String reason) {
        Reservation reservation = activeReservation(reservationId);

        // booking reference alone is guessable, so also check the guest's email
        if (email == null
                || reservation.getGuest() == null
                || reservation.getGuest().getEmail() == null
                || !reservation.getGuest().getEmail().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Booking reference and email do not match.");
        }

        String guestEmail = reservation.getGuest().getEmail(); // registered email
        long days = ChronoUnit.DAYS.between(LocalDate.now(), reservation.getCheckInDate());

        if (days < 0) {
            throw new IllegalStateException("The check-in date has already passed.");
        }

        if (days <= 2) {
            Cancellation rejected = create(reservation, Cancellation.RequestedBy.Guest, reason,
                    Cancellation.CancellationStatus.REJECTED);
            mailService.send(guestEmail, "Cancellation request rejected",
                    "Your cancellation request for reservation #" + reservationId
                            + " could not be accepted because check-in is within 2 days. "
                            + "Your booking remains active.");
            return rejected;
        }

        Cancellation c = create(
                reservation,
                Cancellation.RequestedBy.Guest,
                reason,
                Cancellation.CancellationStatus.REQUESTED
        );

        mailService.send(
                guestEmail,
                "Cancellation request received",
                "We received your cancellation request for reservation #"
                        + reservationId
                        + ". The owner will review it and you will be notified of the outcome."
        );

        return c;
    }

    // ---------- ADMIN: approve / reject a guest request ----------
    @Transactional
    public Cancellation approveCancellation(String cancellationId) {
        Cancellation c = getCancellation(cancellationId);
        if (c.getCancellationStatus() != Cancellation.CancellationStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED cancellations can be approved.");
        }
        Reservation reservation = activeReservation(c.getReservationId());

        // refund policy uses the date the guest asked, not the date admin clicked
        applyCancellation(reservation, c.getRequestedDateTime().toLocalDate());

        c.setCancellationStatus(Cancellation.CancellationStatus.APPROVED);
        return cancellationRepository.save(c);
    }

    @Transactional
    public Cancellation rejectCancellation(String cancellationId) {
        Cancellation c = getCancellation(cancellationId);
        if (c.getCancellationStatus() != Cancellation.CancellationStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED cancellations can be rejected.");
        }
        Reservation reservation = reservationRepository.findById(c.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + c.getReservationId()));

        c.setCancellationStatus(Cancellation.CancellationStatus.REJECTED);
        Cancellation saved = cancellationRepository.save(c);

        mailService.send(reservation.getGuest().getEmail(), "Cancellation request rejected",
                "Your cancellation request for reservation #" + reservation.getReservationId()
                        + " was not approved. Your booking remains active.");
        return saved;
    }

    // ---------- ADMIN: cancel directly (saved as APPROVED straight away) ----------
    @Transactional
    public Cancellation adminCancel(Integer reservationId, String reason) {
        Reservation reservation = activeReservation(reservationId);
        Cancellation c = create(reservation, Cancellation.RequestedBy.Admin, reason,
                Cancellation.CancellationStatus.APPROVED);
        applyCancellation(reservation, LocalDate.now());
        return c;
    }

    public Cancellation getCancellation(String cancellationId) {
        return cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancellation not found: " + cancellationId));
    }

    // Admin portal "Cancellation section": guest + admin cancellations, optional status filter
    public List<Cancellation> getCancellations(Cancellation.CancellationStatus status) {
        return status == null
                ? cancellationRepository.findAll()
                : cancellationRepository.findByCancellationStatus(status);
    }

    // ---------- helpers ----------
    private Reservation activeReservation(Integer reservationId) {
        if (reservationId == null) throw new IllegalArgumentException("Reservation ID is required.");
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationId));
        if (r.getReservationStatus() != ReservationStatus.PENDING
                && r.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("This reservation cannot be cancelled.");
        }
        return r;
    }

    private Cancellation create(
            Reservation r,
            Cancellation.RequestedBy by,
            String reason,
            Cancellation.CancellationStatus status) {

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required.");
        }

        // Get all previous cancellation records for this reservation.
        List<Cancellation> existing =
                cancellationRepository.findAllByReservationId(
                        r.getReservationId());

        // Preserve history and prevent duplicate active requests.
        for (Cancellation old : existing) {
            if (old.getCancellationStatus()
                    == Cancellation.CancellationStatus.REQUESTED) {
                throw new IllegalStateException(
                        "A cancellation request is already pending.");
            }

            if (old.getCancellationStatus()
                    == Cancellation.CancellationStatus.APPROVED) {
                throw new IllegalStateException(
                        "This reservation has already been cancelled.");
            }
        }

        // Do not delete rejected records.
        return cancellationRepository.save(new Cancellation(
                UUID.randomUUID().toString(),
                r.getReservationId(),
                by,
                reason.trim(),
                status
        ));
    }

    // Shared by guest approval and direct admin cancellation:
    // cancel the reservation, create the refund if applicable, email the guest
    private void applyCancellation(Reservation reservation, LocalDate requestDate) {
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        String refundText = refundService.createRefundIfApplicable(reservation, requestDate)
                .map(refund -> "A refund of LKR " + refund.getRefundAmount()
                        + " is pending. The owner will process the payment manually.")
                .orElse("No refund is applicable.");

        mailService.send(reservation.getGuest().getEmail(), "Reservation cancelled",
                "Your reservation #" + reservation.getReservationId()
                        + " has been cancelled. " + refundText);
    }
}