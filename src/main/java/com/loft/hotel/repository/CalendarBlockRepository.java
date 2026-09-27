package com.loft.hotel.repository;

import com.loft.hotel.entity.CalendarBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarBlockRepository extends JpaRepository<CalendarBlock, Integer> {

    // Checks for blocked dates in a given range for a specific room
    @Query("SELECT cb FROM CalendarBlock cb WHERE cb.room.roomId = :roomId " +
            "AND cb.blockedDate BETWEEN :startDate AND :endDate")
    List<CalendarBlock> findBlocksInRangeForRoom(@Param("roomId") Integer roomId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    //Retrieves all calendar blocks across all rooms within a date window.
    List<CalendarBlock> findByBlockedDateBetween(LocalDate startDate, LocalDate endDate);

    // Retrieves calendar blocks for a single room on a specific date. (Used when reservation cancelled release the block date)
    List<CalendarBlock> findByRoom_RoomIdAndBlockedDate(Integer roomId, LocalDate blockedDate);

    // Check if an external iCal event has already been imported to prevent duplicates
    boolean existsByExternalUid(String externalUid);
}
