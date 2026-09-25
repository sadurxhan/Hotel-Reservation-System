package com.loft.hotel.controller;

import com.loft.hotel.model.CalendarBlock;
import com.loft.hotel.service.CalendarService;
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

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
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
}