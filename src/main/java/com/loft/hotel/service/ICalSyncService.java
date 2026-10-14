package com.loft.hotel.service;

import com.loft.hotel.entity.CalendarBlock;
import com.loft.hotel.entity.Room;
import com.loft.hotel.repository.CalendarBlockRepository;
import com.loft.hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Background service for synchronizing external iCal feeds from Airbnb and Booking.com into the local calendar_block inventory
 */
@Service
public class ICalSyncService {

    private static final Logger log = LoggerFactory.getLogger(ICalSyncService.class);
    private static final DateTimeFormatter ICAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CalendarBlockRepository calendarBlockRepository;
    private final RoomRepository roomRepository;

    public ICalSyncService(CalendarBlockRepository calendarBlockRepository,
                           RoomRepository roomRepository) {
        this.calendarBlockRepository = calendarBlockRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Scheduled Job: Runs automatically every 6 hours (21,600,000 milliseconds).
     * initialDelay = 60000 (waits 1 minute after application startup before running the first time).
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 60000)
    public void runPeriodicSync() {
        log.info("Starting scheduled background iCal calendar synchronization...");

        // When production feed URLs are configured, call:
        // syncFromUrl("https://www.airbnb.com/calendar/ical/YOUR_FEED.ics", null, "Airbnb");
    }

    /**
     * Parses an external iCalendar (.ics) stream and persists new blocked dates.
     */
    @Transactional
    public int syncFromUrl(String icalUrl, Integer roomId, String platformName) {
        int newlyBlockedDays = 0;
        Room room = (roomId != null) ? roomRepository.findById(roomId).orElse(null) : null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(icalUrl).toURL().openStream()))) {
            String line;
            String uid = null;
            LocalDate checkIn = null;
            LocalDate checkOut = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                //Capture Unique Event Identifier
                if (line.startsWith("UID:")) {
                    uid = line.substring(4).trim();
                }

                //Capture Start Date
                else if (line.startsWith("DTSTART")) {
                    String rawDate = extractIcalDate(line);
                    if (rawDate != null) {
                        checkIn = LocalDate.parse(rawDate, ICAL_DATE_FORMATTER);
                    }
                }

                //Capture End Date
                else if (line.startsWith("DTEND")) {
                    String rawDate = extractIcalDate(line);
                    if (rawDate != null) {
                        checkOut = LocalDate.parse(rawDate, ICAL_DATE_FORMATTER);
                    }
                }

                //End of VEVENT block: Process the dates
                else if (line.equals("END:VEVENT")) {
                    if (uid != null && checkIn != null && checkOut != null) {

                        // Check if this external booking has already been imported
                        boolean alreadyImported = calendarBlockRepository.existsByExternalUid(uid);

                        if (!alreadyImported) {
                            // Half-open interval loop: [checkIn, checkOut)
                            // The guest occupies nights up to checkout morning
                            for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
                                CalendarBlock block = new CalendarBlock();
                                block.setBlockedDate(d);
                                block.setSource("EXTERNAL_ICAL: " + (platformName != null ? platformName : "OTA"));
                                block.setExternalUid(uid);
                                block.setRoom(room);

                                calendarBlockRepository.save(block);
                                newlyBlockedDays++;
                            }
                            log.info("Imported external stay: UID={} from {} to {} ({} nights)",
                                    uid, checkIn, checkOut, newlyBlockedDays);
                        }
                    }

                    //Reset variables for next VEVENT in the feed
                    uid = null;
                    checkIn = null;
                    checkOut = null;
                }
            }
        } catch (Exception e) {
            log.warn("Unable to sync iCal feed from URL {}: {}", icalUrl, e.getMessage());
        }

        return newlyBlockedDays;
    }

    /**
     * Helper function to extract the 8-digit date string (yyyyMMdd) from iCal date properties.
     */
    private String extractIcalDate(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx != -1 && line.length() >= colonIdx + 9) {
            return line.substring(colonIdx + 1, colonIdx + 9);
        }
        return null;
    }
}