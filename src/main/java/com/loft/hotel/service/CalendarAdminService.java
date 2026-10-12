package com.loft.hotel.service;

import com.loft.hotel.entity.CalendarBlock;
import com.loft.hotel.entity.Room;
import com.loft.hotel.repository.CalendarBlockRepository;
import com.loft.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling maintenance locks and administrative calendar blocking.
 * Domain Rule: The Loft by the Lake operates as an exclusive villa.
 * Even if an administrative block specifies a single room for repairs,
 * any block on that date locks the entire property from guest bookings.
 */
@Service
public class CalendarAdminService {

    private final CalendarBlockRepository calendarBlockRepository;
    private final RoomRepository roomRepository;

    public CalendarAdminService(CalendarBlockRepository calendarBlockRepository,
                                RoomRepository roomRepository) {
        this.calendarBlockRepository = calendarBlockRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Blocks a date interval for maintenance, repairs, or private owner use.
     *
     * @param roomId    Specify which room needs repairs (for owner records). Pass null for property-wide maintenance/private stay.
     * @param startDate First day of the maintenance window (inclusive)
     * @param endDate   Last day of the maintenance window (inclusive)
     * @param reason    Admin description (e.g., "Pool Maintenance", "Room 1 AC Repair")
     * @return List of persisted CalendarBlock records
     */
    @Transactional
    public List<CalendarBlock> blockDateRange(Integer roomId, LocalDate startDate, LocalDate endDate, String reason) {

        //check validity of start and end date
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }

        //Fetch Room metadata if the admin specified a room for repair logs
        Room targetRoom = null;
        if (roomId != null) {
            targetRoom = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));
        }

        //Compose source tag
        String sourceTag = (reason != null && !reason.trim().isEmpty())
                ? "MANUAL_ADMIN: " + reason.trim()
                : "MANUAL_ADMIN";

        List<CalendarBlock> savedBlocks = new ArrayList<>();

        //Note: repairs block the entire calendar day including endDate
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            //Check if this date already has any block recorded
            //Note: One block is enough to make the day unavailable
            boolean alreadyBlocked = (roomId != null)
                    ? calendarBlockRepository.existsByRoom_RoomIdAndBlockedDate(roomId, date)
                    : calendarBlockRepository.existsByBlockedDate(date);

            if (alreadyBlocked) {
                continue; //to avoid duplicate database records
            }

            CalendarBlock block = new CalendarBlock();
            block.setBlockedDate(date);
            block.setSource(sourceTag);
            block.setRoom(targetRoom);

            savedBlocks.add(calendarBlockRepository.save(block));
        }

        return savedBlocks;
    }

    /**
     * Unblocks a date range or specific date.
     *
     * @param roomId Optional room filter (null removes all blocks on that date)
     * @param date   The date to reopen
     */
    @Transactional
    public void unblockDate(Integer roomId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }

        List<CalendarBlock> blocksToDelete;
        if (roomId != null) {
            blocksToDelete = calendarBlockRepository.findByRoom_RoomIdAndBlockedDate(roomId, date);
        } else {
            blocksToDelete = calendarBlockRepository.findByBlockedDateBetween(date, date);
        }

        if (!blocksToDelete.isEmpty()) {
            calendarBlockRepository.deleteAll(blocksToDelete);
        }
    }
}