package com.loft.hotel.service;

import com.loft.hotel.model.CalendarBlock;
import com.loft.hotel.repository.CalendarBlockRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalendarService {

    private final CalendarBlockRepository calendarBlockRepository;

    // Constructor injection: Spring injects the repository automatically
    public CalendarService(CalendarBlockRepository calendarBlockRepository) {
        this.calendarBlockRepository = calendarBlockRepository;
    }

    /**
     * Checks if a room (or the full villa) is available for a requested date range.
     * Returns true if no calendar blocks overlap with the stay.
     */
    public boolean isRoomAvailable(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Invalid date range: check-out must be after check-in.");
        }

        List<CalendarBlock> blocks = calendarBlockRepository.findBlocksInRangeForRoom(checkIn, checkOut, roomId);
        return blocks.isEmpty();
    }

    /**
     * Creates and saves a new calendar block (e.g. manual admin block).
     */
    public CalendarBlock createBlock(CalendarBlock block) {
        if (block.getBlockedDate() == null) {
            throw new IllegalArgumentException("Blocked date cannot be null.");
        }
        return calendarBlockRepository.save(block);
    }

    /**
     * Retrieves all blocked dates within a specific window (used for the frontend calendar view).
     */
    public List<CalendarBlock> getBlocksForPeriod(LocalDate startDate, LocalDate endDate) {
        return calendarBlockRepository.findByBlockedDateBetween(startDate, endDate);
    }
}