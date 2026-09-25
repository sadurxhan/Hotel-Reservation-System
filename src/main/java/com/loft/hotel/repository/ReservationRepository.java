package com.loft.hotel.repository;

import com.loft.hotel.entity.Reservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory storage for reservations.
 *
 * Data will be cleared when the Spring Boot application is restarted.
 */
@Repository
public class ReservationRepository {

    private final Map<Long, Reservation> store = new LinkedHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public Reservation save(Reservation reservation) {

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

    /**
     * Checks whether the requested dates overlap
     * with an existing reservation.
     *
     * CANCELLED reservations do not block the dates.
     */
    public boolean hasOverlap(LocalDateRange range) {

        return store.values().stream().anyMatch(reservation ->

                !"CANCELLED".equals(reservation.getStatus())

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