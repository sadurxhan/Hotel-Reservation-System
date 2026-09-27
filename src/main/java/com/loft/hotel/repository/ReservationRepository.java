package com.loft.hotel.repository;

import com.loft.hotel.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    // Finds reservations for a specific room that overlap the given date range
    // and are NOT cancelled - used to check availability before booking.
    @Query("""
        select r from Reservation r
        join ReservationRoomSelection s on s.reservation = r
        where s.room.roomId = :roomId
          and r.reservationStatus <> com.loft.hotel.entity.ReservationStatus.CANCELLED
          and r.checkInDate < :checkOut
          and r.checkOutDate > :checkIn
        """)
    List<Reservation> findOverlappingForRoom(@Param("roomId") Integer roomId,
                                             @Param("checkIn") LocalDate checkIn,
                                             @Param("checkOut") LocalDate checkOut);
}
