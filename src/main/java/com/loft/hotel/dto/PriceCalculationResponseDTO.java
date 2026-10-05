package com.loft.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for returning calculated pricing details.
 * Transports pricing calculations from the Service layer to Controllers and UI
 * without exposing internal database entities directly.
 */
public class PriceCalculationResponseDTO {

    private Integer roomId;
    private int numberOfNights;
    private BigDecimal baseNightlyRate;
    private BigDecimal totalCalculatedPrice;

    // Detailed itemization for each night of the stay
    private List<NightlyDetail> dailyBreakdown = new ArrayList<>();

    // Default constructor for JSON serialization
    public PriceCalculationResponseDTO() {
    }

    // parameterized constructor
    public PriceCalculationResponseDTO(Integer roomId, int numberOfNights, BigDecimal baseNightlyRate, BigDecimal totalCalculatedPrice, List<NightlyDetail> dailyBreakdown) {
        this.roomId = roomId;
        this.numberOfNights = numberOfNights;
        this.baseNightlyRate = baseNightlyRate;
        this.totalCalculatedPrice = totalCalculatedPrice;
        this.dailyBreakdown = dailyBreakdown;
    }

    /**
     * Inner class representing the price breakdown for a single night.
     */
    public static class NightlyDetail {
        private LocalDate date;
        private BigDecimal baseRate;
        private BigDecimal multiplier;
        private BigDecimal effectiveRate;
        private String appliedPackage; // e.g., "Standard Rate", "Weekend Surge +15%"

        public NightlyDetail() {
        }

        public NightlyDetail(LocalDate date, BigDecimal baseRate, BigDecimal multiplier,
                             BigDecimal effectiveRate, String appliedPackage) {
            this.date = date;
            this.baseRate = baseRate;
            this.multiplier = multiplier;
            this.effectiveRate = effectiveRate;
            this.appliedPackage = appliedPackage;
        }

        // Getters and Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public BigDecimal getBaseRate() { return baseRate; }
        public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }

        public BigDecimal getMultiplier() { return multiplier; }
        public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }

        public BigDecimal getEffectiveRate() { return effectiveRate; }
        public void setEffectiveRate(BigDecimal effectiveRate) { this.effectiveRate = effectiveRate; }

        public String getAppliedPackage() { return appliedPackage; }
        public void setAppliedPackage(String appliedPackage) { this.appliedPackage = appliedPackage; }
    }

    // Getters and Setters for the main DTO
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public int getNumberOfNights() { return numberOfNights; }
    public void setNumberOfNights(int numberOfNights) { this.numberOfNights = numberOfNights; }

    public BigDecimal getBaseNightlyRate() { return baseNightlyRate; }
    public void setBaseNightlyRate(BigDecimal baseNightlyRate) { this.baseNightlyRate = baseNightlyRate; }

    public BigDecimal getTotalCalculatedPrice() { return totalCalculatedPrice; }
    public void setTotalCalculatedPrice(BigDecimal totalCalculatedPrice) { this.totalCalculatedPrice = totalCalculatedPrice; }

    public List<NightlyDetail> getDailyBreakdown() { return dailyBreakdown; }
    public void setDailyBreakdown(List<NightlyDetail> dailyBreakdown) { this.dailyBreakdown = dailyBreakdown; }
}