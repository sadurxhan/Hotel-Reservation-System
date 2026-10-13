package com.loft.hotel.repository;

import com.loft.hotel.entity.ReservationRoomSelection;
import java.util.List;
import com.loft.hotel.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRoomSelectionRepository extends JpaRepository<ReservationRoomSelection, Integer> {
    List<ReservationRoomSelection> findByReservation(Reservation reservation);
}
