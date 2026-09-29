package com.loft.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "calendar_block")
public class CalendarBlock {

    // Primary Key (Auto-incremented unique ID for every block record)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;

    // The date that is unavailable for booking
    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    // Identifies who or what blocked it ('MANUAL_ADMIN', 'AIRBNB_ICAL', 'BOOKING_COM')
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    // UID from external iCal feeds (e.g., Airbnb UID) to avoid duplicating imported blocks
    @Column(name = "external_uid", length = 255)
    private String externalUid;

    // Many blocked dates can belong to one specific room. (If null, the entire property is blocked.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    // Default constructor
    public CalendarBlock() {}

    public CalendarBlock(LocalDate blockedDate, String source, String externalUid, Room room) {
        this.blockedDate = blockedDate;
        this.source = source;
        this.externalUid = externalUid;
        this.room = room;
    }

    // Getters and Setters
    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public LocalDate getBlockedDate() { return blockedDate; }
    public void setBlockedDate(LocalDate blockedDate) { this.blockedDate = blockedDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalUid() { return externalUid; }
    public void setExternalUid(String externalUid) { this.externalUid = externalUid; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
}