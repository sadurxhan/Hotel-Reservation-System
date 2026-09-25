package com.loft.hotel.repository;

import com.loft.hotel.entity.Reservation;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory storage for now (no database set up yet).
 * Everything resets when you restart the app - that's expected at this stage.
 */
@Repository
public class ReservationRepository {

    private final Map<Long, Reservation> store = new LinkedHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Reservation save(Reservation r) {
        if (r.getId() == null) {
            r.setId(idCounter.getAndIncrement());
        }
        store.put(r.getId(), r);
        return r;
    }

    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(store.values());
    }

    /** True if any existing reservation's dates overlap the requested range. */
    public boolean hasOverlap(LocalDateRange range) {
        return store.values().stream().anyMatch(r ->
                r.getCheckInDate().isBefore(range.checkOut()) &&
                        r.getCheckOutDate().isAfter(range.checkIn()));
    }

    public record LocalDateRange(java.time.LocalDate checkIn, java.time.LocalDate checkOut) {}
}