package com.loft.hotel.service;

import com.loft.hotel.dto.PriceCalculationResponseDTO;
import com.loft.hotel.dto.PriceCalculationResponseDTO.NightlyDetail;
import com.loft.hotel.entity.RateOverride;
import com.loft.hotel.entity.Room;
import com.loft.hotel.entity.RoomPricingTier;
import com.loft.hotel.repository.RateOverrideRepository;
import com.loft.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling dynamic pricing calculations
 * Purpose: Decouples pricing calculation algorithms from the presentation tier (Controllers)
 * and storage tier (Entities/Repositories), fulfilling the Single Responsibility Principle (SRP).
 */
@Service
public class PricingEngineService {

    // Dependencies injected via Constructor Injection
    private final RateOverrideRepository rateOverrideRepository;
    private final RoomRepository roomRepository;

    public PricingEngineService(RateOverrideRepository rateOverrideRepository,
                                RoomRepository roomRepository) {
        this.rateOverrideRepository = rateOverrideRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Calculates the dynamic nightly rate across the stay duration.
     *
     * @param roomId       The room ID selected
     * @param checkInDate  Guest arrival date
     * @param checkOutDate Guest departure date
     * @return PriceCalculationResponseDTO containing nightly breakdowns and total cost
     */
    public PriceCalculationResponseDTO calculatePrice(Integer roomId, LocalDate checkInDate, LocalDate checkOutDate) {

        //Business Invariant Validation: Check-out must strictly be after check-in
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be strictly after check-in date.");
        }

        //Fetch Room & associated Pricing Tier
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        //Matches Room.java: getTier()
        RoomPricingTier tier = room.getTier();
        if (tier == null) {
            throw new IllegalStateException("No pricing tier assigned to Room ID: " + roomId);
        }

        BigDecimal baseRate = tier.getBaseRate();

        //Batch Retrieval of Overrides:
        // Execute a single repository call to fetch all active overrides in the interval.
        List<RateOverride> activeOverrides = rateOverrideRepository.findActiveOverridesInRange(
                checkInDate, checkOutDate, roomId
        );

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<NightlyDetail> dailyBreakdown = new ArrayList<>();
        int nightsCount = 0;

        //Guests stay for nights. Check-out day does not count as a billed night.
        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            nightsCount++;

            BigDecimal currentMultiplier = BigDecimal.ONE; // 1.0 = standard default base rate
            String appliedPackageName = "Standard Rate";

            // Check if any active promotional or surge package wraps this specific night
            for (RateOverride override : activeOverrides) {
                // Interval check: startDate <= current night <= endDate
                if (!date.isBefore(override.getStartDate()) && !date.isAfter(override.getEndDate())) {
                    currentMultiplier = override.getRateMultiplier();
                    appliedPackageName = override.getPackageName();
                    break; // Apply active override for this night
                }
            }

            //Calculate Effective Nightly Rate = baseRate * multiplier
            BigDecimal effectiveNightlyRate = baseRate.multiply(currentMultiplier).setScale(2, RoundingMode.HALF_UP);
            grandTotal = grandTotal.add(effectiveNightlyRate);

            //Record line-item entry
            dailyBreakdown.add(new NightlyDetail(date, baseRate, currentMultiplier, effectiveNightlyRate, appliedPackageName));
        }

        //Encapsulate results into the DTO
        return new PriceCalculationResponseDTO(roomId, nightsCount, baseRate, grandTotal, dailyBreakdown);
    }
}