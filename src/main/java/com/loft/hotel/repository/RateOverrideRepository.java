package com.loft.hotel.repository;

import com.loft.hotel.model.RateOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RateOverrideRepository extends JpaRepository<RateOverride, Integer> {

    // Fetch all active overrides that overlap with a requested date window
    @Query("SELECT ro FROM RateOverride ro WHERE ro.isActive = true " +
            "AND ro.startDate <= :endDate AND ro.endDate >= :startDate " +
            "AND (ro.room.roomId = :roomId OR ro.room IS NULL)")
    List<RateOverride> findActiveOverridesInRange(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("roomId") Long roomId);

    // List all active packages (for admin view / promotions page)
    List<RateOverride> findByIsActiveTrue();
}
