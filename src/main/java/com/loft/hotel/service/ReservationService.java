package com.loft.hotel.service;

import com.loft.hotel.dto.BookingRequest;
import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomSelectionRepository selectionRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationRoomSelectionRepository selectionRepository,
                              GuestRepository guestRepository,
                              RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.selectionRepository = selectionRepository;
        this.guestRepository = guestRepository;
        this.roomRepository = roomRepository;
    }

    // True if the given room has no overlapping, non-cancelled reservation in that range.
    public boolean isRoomAvailable(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }
        List<Reservation> overlaps = reservationRepository.findOverlappingForRoom(roomId, checkIn, checkOut);
        return overlaps.isEmpty();
    }

    // Creates the Guest (or reuses an existing one by email), the Reservation,
    // and the ReservationRoomSelection linking them to a specific room - all in one booking.
    public synchronized Reservation createReservation(BookingRequest request) {

        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Guest name is required.");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Guest email is required.");
        }
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("Please choose a room.");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Please choose both check-in and check-out dates.");
        }
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("That room does not exist."));

        if (!isRoomAvailable(room.getRoomId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is already booked for those dates.");
        }

        // Reuse the guest record if this email already booked before, otherwise create one.
        Guest guest = guestRepository.findByEmail(request.getGuestEmail())
                .orElseGet(() -> {
                    Guest g = new Guest();
                    String[] parts = request.getGuestName().trim().split("\\s+", 2);
                    g.setFname(parts[0]);
                    g.setLname(parts.length > 1 ? parts[1] : "");
                    g.setEmail(request.getGuestEmail());
                    g.setPhoneNo(request.getGuestPhone());
                    return guestRepository.save(g);
                });

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal pricePerNight = room.getTier().getBaseRate();
        BigDecimal totalAmount = pricePerNight.multiply(BigDecimal.valueOf(nights));

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfGuests(request.getNumberOfGuests() != null ? request.getNumberOfGuests() : 1);
        reservation.setTotalAmount(totalAmount);
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation = reservationRepository.save(reservation);

        ReservationRoomSelection selection = new ReservationRoomSelection();
        selection.setReservation(reservation);
        selection.setRoom(room);
        selection.setNoOfGuests(reservation.getNumberOfGuests());
        selection.setRoomPricePerNight(pricePerNight);
        selectionRepository.save(selection);

        return reservation;
    }

    public Reservation getById(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation " + id + " not found"));
    }

    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    public Reservation confirm(Integer id) {
        Reservation r = getById(id);
        if (r.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed.");
        }
        r.setReservationStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(r);
    }

    public Reservation cancel(Integer id) {
        Reservation r = getById(id);
        if (r.getReservationStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled.");
        }
        r.setReservationStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(r);
    }
}
