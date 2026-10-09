package com.loft.hotel.service;

import com.loft.hotel.dto.BookingRequest;
import com.loft.hotel.entity.Guest;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationRoomSelection;
import com.loft.hotel.entity.ReservationStatus;
import com.loft.hotel.entity.Room;
import com.loft.hotel.repository.GuestRepository;
import com.loft.hotel.repository.ReservationRepository;
import com.loft.hotel.repository.ReservationRoomSelectionRepository;
import com.loft.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationRoomSelectionRepository selectionRepository,
            GuestRepository guestRepository,
            RoomRepository roomRepository) {

        this.reservationRepository = reservationRepository;
        this.selectionRepository = selectionRepository;
        this.guestRepository = guestRepository;
        this.roomRepository = roomRepository;
    }

    // --------------------------------------------------
    // CHECK ROOM AVAILABILITY
    // --------------------------------------------------

    public boolean isRoomAvailable(
            Integer roomId,
            LocalDate checkIn,
            LocalDate checkOut) {

        if (roomId == null ||
                checkIn == null ||
                checkOut == null) {

            return false;
        }

        if (!checkOut.isAfter(checkIn)) {
            return false;
        }

        List<Reservation> overlaps =
                reservationRepository.findOverlappingForRoom(
                        roomId,
                        checkIn,
                        checkOut
                );

        return overlaps.isEmpty();
    }

    // --------------------------------------------------
    // CREATE RESERVATION
    // --------------------------------------------------

    @Transactional
    public synchronized Reservation createReservation(
            BookingRequest request) {

        // -----------------------------
        // 0. Request Validation
        // -----------------------------

        if (request == null) {
            throw new IllegalArgumentException(
                    "Booking request cannot be empty.");
        }

        // -----------------------------
        // 1. Guest Name Validation
        // -----------------------------

        if (request.getGuestName() == null ||
                request.getGuestName().isBlank()) {

            throw new IllegalArgumentException(
                    "Guest name is required.");
        }

        // -----------------------------
        // 2. Email Validation
        // -----------------------------

        if (request.getGuestEmail() == null ||
                request.getGuestEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Guest email is required.");
        }

        if (!request.getGuestEmail().matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new IllegalArgumentException(
                    "Please enter a valid email address.");
        }

        // -----------------------------
        // 3. Phone Number Validation
        // -----------------------------

        if (request.getGuestPhone() == null ||
                request.getGuestPhone().isBlank()) {

            throw new IllegalArgumentException(
                    "Guest phone number is required.");
        }

        if (!request.getGuestPhone().matches(
                "^\\+?[0-9]{10,15}$")) {

            throw new IllegalArgumentException(
                    "Please enter a valid phone number (10-15 digits).");
        }

        // -----------------------------
        // 4. Room Selection Validation
        // -----------------------------

        if (request.getRoomId() == null) {

            throw new IllegalArgumentException(
                    "Please choose a room.");
        }

        // -----------------------------
        // 5. Date Validation
        // -----------------------------

        if (request.getCheckInDate() == null ||
                request.getCheckOutDate() == null) {

            throw new IllegalArgumentException(
                    "Please choose both check-in and check-out dates.");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Check-in date cannot be in the past.");
        }

        if (!request.getCheckOutDate()
                .isAfter(request.getCheckInDate())) {

            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date.");
        }

        // -----------------------------
        // 6. Number of Guests Validation
        // -----------------------------

        if (request.getNumberOfGuests() == null ||
                request.getNumberOfGuests() < 1) {

            throw new IllegalArgumentException(
                    "Number of guests must be at least 1.");
        }

        // -----------------------------
        // 7. Find Room
        // -----------------------------

        Room room = roomRepository
                .findById(request.getRoomId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "That room does not exist."));

        // -----------------------------
        // 8. Check Room Is Active
        // -----------------------------

        if (room.getIsActive() != null &&
                !room.getIsActive()) {

            throw new IllegalArgumentException(
                    "That room is currently unavailable.");
        }

        // -----------------------------
        // 9. Check Room Availability
        // -----------------------------

        if (!isRoomAvailable(
                room.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate())) {

            throw new IllegalArgumentException(
                    "Room " + room.getRoomNumber()
                            + " is already booked for those dates.");
        }

        // -----------------------------
        // 10. Find or Create Guest
        // -----------------------------

        Guest guest = guestRepository
                .findByEmail(request.getGuestEmail())
                .orElseGet(() -> {

                    Guest g = new Guest();

                    String[] parts = request.getGuestName()
                            .trim()
                            .split("\\s+", 2);

                    g.setFname(parts[0]);

                    g.setLname(
                            parts.length > 1
                                    ? parts[1]
                                    : ""
                    );

                    g.setEmail(request.getGuestEmail());

                    g.setPhoneNo(request.getGuestPhone());

                    return guestRepository.save(g);
                });

        // -----------------------------
        // 11. Calculate Number of Nights
        // -----------------------------

        long nights = ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        // -----------------------------
        // 12. Get Room Price
        // -----------------------------

        BigDecimal pricePerNight =
                room.getTier().getBaseRate();

        // -----------------------------
        // 13. Calculate Total Amount
        // -----------------------------

        BigDecimal totalAmount =
                pricePerNight.multiply(
                        BigDecimal.valueOf(nights)
                );

        // -----------------------------
        // 14. Create Reservation
        // -----------------------------

        Reservation reservation =
                new Reservation();

        reservation.setGuest(guest);

        reservation.setCheckInDate(
                request.getCheckInDate()
        );

        reservation.setCheckOutDate(
                request.getCheckOutDate()
        );

        reservation.setNumberOfGuests(
                request.getNumberOfGuests()
        );

        reservation.setTotalAmount(
                totalAmount
        );

        reservation.setReservationStatus(
                ReservationStatus.PENDING
        );

        // -----------------------------
        // 15. Save Reservation
        // -----------------------------

        reservation =
                reservationRepository.save(reservation);

        // -----------------------------
        // 16. Create Room Selection
        // -----------------------------

        ReservationRoomSelection selection =
                new ReservationRoomSelection();

        selection.setReservation(reservation);

        selection.setRoom(room);

        selection.setNoOfGuests(
                reservation.getNumberOfGuests()
        );

        selection.setRoomPricePerNight(
                pricePerNight
        );

        // -----------------------------
        // 17. Save Room Selection
        // -----------------------------

        selectionRepository.save(selection);

        // -----------------------------
        // 18. Return Reservation
        // -----------------------------

        return reservation;
    }

    // --------------------------------------------------
    // GET RESERVATION BY ID
    // --------------------------------------------------

    public Reservation getById(Integer id) {

        return reservationRepository
                .findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Reservation "
                                        + id
                                        + " not found"));
    }

    // --------------------------------------------------
    // GET ALL RESERVATIONS
    // --------------------------------------------------

    public List<Reservation> getAll() {

        return reservationRepository.findAll();
    }

    // --------------------------------------------------
    // CONFIRM RESERVATION
    // --------------------------------------------------

    @Transactional
    public Reservation confirm(Integer id) {

        Reservation reservation =
                getById(id);

        if (reservation.getReservationStatus()
                != ReservationStatus.PENDING) {

            throw new IllegalStateException(
                    "Only PENDING reservations can be confirmed.");
        }

        reservation.setReservationStatus(
                ReservationStatus.CONFIRMED
        );

        return reservationRepository.save(
                reservation
        );
    }

    // --------------------------------------------------
    // CANCEL RESERVATION
    // --------------------------------------------------

    @Transactional
    public Reservation cancel(Integer id) {

        Reservation reservation =
                getById(id);

        if (reservation.getReservationStatus()
                == ReservationStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Reservation is already cancelled.");
        }

        reservation.setReservationStatus(
                ReservationStatus.CANCELLED
        );

        return reservationRepository.save(
                reservation
        );
    }
}