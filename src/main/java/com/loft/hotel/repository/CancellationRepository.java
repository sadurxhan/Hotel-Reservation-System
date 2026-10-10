package com.loft.hotel.repository;

import com.loft.hotel.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, String> {
    Optional<Cancellation> findByReservationId(Integer reservationId);
    List<Cancellation> findByCancellationStatus(Cancellation.CancellationStatus status);
}