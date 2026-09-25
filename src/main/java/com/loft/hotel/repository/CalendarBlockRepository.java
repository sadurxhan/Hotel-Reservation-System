package com.loft.hotel.repository;

import com.loft.hotel.model.CalendarBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarBlockRepository extends JpaRepository<CalendarBlock, Integer> {

    // Fetch all blocks between check-in and check-out for a specific room or entire property (room IS NULL)
    @Query("SELECT cb FROM CalendarBlock cb WHERE cb.blockedDate >= :startDate AND cb.blockedDate < :endDate " +
            "AND (cb.room.roomId = :roomId OR cb.room IS NULL)")
    List<CalendarBlock> findBlocksInRangeForRoom(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("roomId") Integer roomId);

    // Fetch all blocks between two dates (used by admin calendar dashboard view)
    List<CalendarBlock> findByBlockedDateBetween(LocalDate startDate, LocalDate endDate);

    // Check if an external iCal event has already been imported to prevent duplicates
    boolean existsByExternalUid(String externalUid);
}
