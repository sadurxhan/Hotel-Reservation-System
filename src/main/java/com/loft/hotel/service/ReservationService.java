package com.loft.hotel.service;

import com.loft.hotel.dto.BookingRequest;
import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
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
    private final CalendarBlockRepository calendarBlockRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationRoomSelectionRepository selectionRepository,
                              GuestRepository guestRepository,
                              RoomRepository roomRepository,
                              CalendarBlockRepository calendarBlockRepository) {
        this.reservationRepository = reservationRepository;
        this.selectionRepository = selectionRepository;
        this.guestRepository = guestRepository;
        this.roomRepository = roomRepository;
        this.calendarBlockRepository = calendarBlockRepository;
    }

    // --------------------------------------------------
    // CHECK ROOM AVAILABILITY
    // --------------------------------------------------
    public boolean isRoomAvailable(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        if (roomId == null || checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }

        // 1. Check reservation overlaps
        List<Reservation> overlaps = reservationRepository.findOverlappingForRoom(roomId, checkIn, checkOut);
        if (!overlaps.isEmpty()) {
            return false;
        }

        // 2. Check calendar blocks (both external sync and manual blocks)
        LocalDate dayBeforeCheckOut = checkOut.minusDays(1);
        List<CalendarBlock> blocks = calendarBlockRepository.findBlocksInRangeForRoom(roomId, checkIn, dayBeforeCheckOut);
        return blocks.isEmpty();
    }

    // --------------------------------------------------
    // CREATE RESERVATION
    // --------------------------------------------------
    @Transactional
    public synchronized Reservation createReservation(BookingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be empty.");
        }

        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Guest name is required.");
        }

        if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Guest email is required.");
        }

        if (!request.getGuestEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        if (request.getGuestPhone() == null || request.getGuestPhone().isBlank()) {
            throw new IllegalArgumentException("Guest phone number is required.");
        }

        if (!request.getGuestPhone().matches("^\\+?[0-9]{10,15}$")) {
            throw new IllegalArgumentException("Please enter a valid phone number (10-15 digits).");
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

        if (request.getNumberOfGuests() == null || request.getNumberOfGuests() < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("That room does not exist."));

        if (room.getIsActive() != null && !room.getIsActive()) {
            throw new IllegalArgumentException("That room is currently unavailable.");
        }

        if (!isRoomAvailable(room.getRoomId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is already blocked or booked for those dates.");
        }

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

        // Safe rate resolution with fallback to prevent NullPointerException
        BigDecimal pricePerNight = (room.getTier() != null && room.getTier().getBaseRate() != null)
                ? room.getTier().getBaseRate()
                : new BigDecimal("35000.00");

        BigDecimal totalAmount = pricePerNight.multiply(BigDecimal.valueOf(nights));

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfGuests(request.getNumberOfGuests());
        reservation.setTotalAmount(totalAmount);
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation = reservationRepository.save(reservation);

        ReservationRoomSelection selection = new ReservationRoomSelection();
        selection.setReservation(reservation);
        selection.setRoom(room);
        selection.setNoOfGuests(reservation.getNumberOfGuests());
        selection.setRoomPricePerNight(pricePerNight);
        selectionRepository.save(selection);

        // Block dates in calendar
        LocalDate currentDate = request.getCheckInDate();
        while (currentDate.isBefore(request.getCheckOutDate())) {
            CalendarBlock block = new CalendarBlock();
            block.setRoom(room);
            block.setBlockedDate(currentDate);
            block.setSource("INTERNAL_BOOKING");
            calendarBlockRepository.save(block);
            currentDate = currentDate.plusDays(1);
        }

        return reservation;
    }

    // --------------------------------------------------
    // GET RESERVATION BY ID
    // --------------------------------------------------
    public Reservation getById(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation " + id + " not found"));
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
        Reservation reservation = getById(id);
        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed.");
        }
        reservation.setReservationStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    // --------------------------------------------------
    // CANCEL RESERVATION
    // --------------------------------------------------
    @Transactional
    public Reservation cancel(Integer id) {
        Reservation reservation = getById(id);
        if (reservation.getReservationStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled.");
        }
        reservation.setReservationStatus(ReservationStatus.CANCELLED);

        // Clear calendar blocks created for this reservation
        List<ReservationRoomSelection> selections = selectionRepository.findAll();
        for (ReservationRoomSelection sel : selections) {
            if (sel.getReservation().getReservationId().equals(reservation.getReservationId())) {
                Integer roomId = sel.getRoom().getRoomId();
                LocalDate date = reservation.getCheckInDate();
                while (date.isBefore(reservation.getCheckOutDate())) {
                    List<CalendarBlock> blocks = calendarBlockRepository.findByRoom_RoomIdAndBlockedDate(roomId, date);
                    calendarBlockRepository.deleteAll(blocks);
                    date = date.plusDays(1);
                }
            }
        }

        return reservationRepository.save(reservation);
    }
}