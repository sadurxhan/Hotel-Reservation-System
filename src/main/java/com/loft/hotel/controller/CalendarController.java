package com.loft.hotel.controller;

import com.loft.hotel.entity.CalendarBlock;
import com.loft.hotel.service.CalendarAdminService;
import com.loft.hotel.service.CalendarService;
import com.loft.hotel.service.ICalSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*") // Allows your frontend to call these endpoints
public class CalendarController {

    // 1. All service dependencies grouped cleanly at the top
    private final CalendarService calendarService;
    private final CalendarAdminService calendarAdminService;
    private final ICalSyncService iCalSyncService;

    // 2. Single constructor that injects all three services at once
    public CalendarController(CalendarService calendarService,
                              CalendarAdminService calendarAdminService,
                              ICalSyncService iCalSyncService) {
        this.calendarService = calendarService;
        this.calendarAdminService = calendarAdminService;
        this.iCalSyncService = iCalSyncService;
    }

    /**
     * Endpoint 1: Check availability for a room between dates
     * URL: GET /api/calendar/check-availability?roomId=1&checkIn=2026-10-01&checkOut=2026-10-05
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam(required = false) Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        boolean isAvailable = calendarService.isRoomAvailable(roomId, checkIn, checkOut);

        return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "roomId", roomId != null ? roomId : "PROPERTY_WIDE",
                "checkIn", checkIn,
                "checkOut", checkOut
        ));
    }

    /**
     * Endpoint 2: Add a manual calendar block (Admin feature)
     * URL: POST /api/calendar/block
     */
    @PostMapping("/block")
    public ResponseEntity<CalendarBlock> addBlock(@RequestBody CalendarBlock block) {
        CalendarBlock created = calendarService.createBlock(block);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Endpoint 3: Fetch all blocks in a date window for rendering the calendar UI
     * URL: GET /api/calendar/blocks?startDate=2026-10-01&endDate=2026-10-31
     */
    @GetMapping("/blocks")
    public ResponseEntity<List<CalendarBlock>> getBlocks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CalendarBlock> blocks = calendarService.getBlocksForPeriod(startDate, endDate);
        return ResponseEntity.ok(blocks);
    }

    /**
     * Endpoint 4: Remove a block on a specific date (Admin unblock feature)
     * URL: DELETE /api/calendar/block?date=2026-10-15&roomId=1
     */
    @DeleteMapping("/block")
    public ResponseEntity<Void> unblockDate(
            @RequestParam(required = false) Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        calendarAdminService.unblockDate(roomId, date);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint 5: Manually trigger an external iCal sync
     * URL: POST /api/calendar/sync-ical
     */
    @PostMapping("/sync-ical")
    public ResponseEntity<Map<String, Object>> triggerIcalSync(
            @RequestParam String icalUrl,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(defaultValue = "Airbnb") String platform) {

        int count = iCalSyncService.syncFromUrl(icalUrl, roomId, platform);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "importedDays", count,
                "platform", platform
        ));
    }
}