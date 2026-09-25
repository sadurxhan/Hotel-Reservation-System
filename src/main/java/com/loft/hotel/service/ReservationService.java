package com.loft.hotel.service;

import com.loft.hotel.entity.Reservation;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        return !repository.hasOverlap(new ReservationRepository.LocalDateRange(checkIn, checkOut));
    }

    /** Validates the booking, checks availability, and saves it as PENDING. */
    public synchronized Reservation createReservation(Reservation r) {
        if (r.getCheckInDate() == null || r.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Please choose both check-in and check-out dates.");
        }
        if (!r.getCheckOutDate().isAfter(r.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
        if (!isAvailable(r.getCheckInDate(), r.getCheckOutDate())) {
            throw new IllegalArgumentException("Those dates are already booked. Please choose different dates.");
        }
        r.setStatus("PENDING");
        return repository.save(r);
    }

    public Reservation getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation " + id + " not found"));
    }

    public List<Reservation> getAll() {
        return repository.findAll();
    }

    /** Called when payment succeeds - moves the reservation from PENDING to CONFIRMED. */
    public Reservation confirm(Long id) {
        Reservation r = getById(id);
        r.setStatus("CONFIRMED");
        return repository.save(r);
    }
}

