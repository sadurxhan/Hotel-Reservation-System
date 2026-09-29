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

    public boolean isRoomAvailable(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return false;
        }

        // Check reservation overlaps
        List<Reservation> overlaps = reservationRepository.findOverlappingForRoom(roomId, checkIn, checkOut);
        if (!overlaps.isEmpty()) {
            return false;
        }

        // Check calendar blocks (both admin blocks and manual blocks)
        LocalDate dayBeforeCheckOut = checkOut.minusDays(1);
        List<CalendarBlock> blocks = calendarBlockRepository.findBlocksInRangeForRoom(roomId, checkIn, dayBeforeCheckOut);
        return blocks.isEmpty();
    }

    @Transactional
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

    @Transactional
    public Reservation cancel(Integer id) {
        Reservation r = getById(id);
        if (r.getReservationStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled.");
        }
        r.setReservationStatus(ReservationStatus.CANCELLED);

        // Clear calendar blocks created for this room during those dates
        List<ReservationRoomSelection> selections = selectionRepository.findAll();
        for (ReservationRoomSelection sel : selections) {
            if (sel.getReservation().getReservationId().equals(r.getReservationId())) {
                Integer roomId = sel.getRoom().getRoomId();
                LocalDate date = r.getCheckInDate();
                while (date.isBefore(r.getCheckOutDate())) {
                    List<CalendarBlock> blocks = calendarBlockRepository.findByRoom_RoomIdAndBlockedDate(roomId, date);
                    calendarBlockRepository.deleteAll(blocks);
                    date = date.plusDays(1);
                }
            }
        }

        return reservationRepository.save(r);
    }
}
