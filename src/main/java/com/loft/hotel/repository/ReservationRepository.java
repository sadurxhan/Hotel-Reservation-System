package com.loft.hotel.repository;

import com.loft.hotel.entity.Reservation;
import com.loft.hotel.entity.ReservationStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// In-memory storage for reservations.
// Data will be cleared when the Spring Boot application is restarted (no database yet).
@Repository
public class ReservationRepository {

    private final Map<Long, Reservation> store = new LinkedHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Reservation save(Reservation reservation) {

        // Only assign a new ID the first time this reservation is saved.
        // If it already has one, this is an update (e.g. confirm/cancel), so keep it.
        if (reservation.getId() == null) {
            reservation.setId(idCounter.getAndIncrement());
        }

        store.put(reservation.getId(), reservation);

        return reservation;
    }

    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(store.values());
    }

    // True if an existing (non-cancelled) reservation overlaps the given date range.
    // CANCELLED reservations are ignored so their dates become bookable again.
    public boolean hasOverlap(LocalDateRange range) {

        return store.values().stream().anyMatch(reservation ->

                reservation.getStatus() != ReservationStatus.CANCELLED

                        && reservation.getCheckInDate()
                        .isBefore(range.checkOut())

                        && reservation.getCheckOutDate()
                        .isAfter(range.checkIn())
        );
    }

    public record LocalDateRange(
            LocalDate checkIn,
            LocalDate checkOut
    ) {
    }
}