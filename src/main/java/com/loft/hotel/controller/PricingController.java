package com.loft.hotel.controller;

import com.loft.hotel.dto.PriceCalculationResponseDTO;
import com.loft.hotel.entity.RateOverride;
import com.loft.hotel.repository.RateOverrideRepository;
import com.loft.hotel.service.PricingEngineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Dynamic Pricing and Rate Overrides.
 */
@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "*") //allows the guest booking UI and admin screens to call these endpoints
public class PricingController {

    private final PricingEngineService pricingEngineService;
    private final RateOverrideRepository rateOverrideRepository;

    //Constructor injection: Spring provides the service and repository automatically
    public PricingController(PricingEngineService pricingEngineService,
                             RateOverrideRepository rateOverrideRepository) {
        this.pricingEngineService = pricingEngineService;
        this.rateOverrideRepository = rateOverrideRepository;
    }

    /**
     * Calculate Dynamic Price for a Stay
     * Called by booking form as soon as dates or rooms are selected.
     *
     * Method: GET
     * URL Example: /api/pricing/calculate?roomId=1&checkIn=2026-11-10&checkOut=2026-11-13
     *
     * @param roomId       Selected room ID
     * @param checkInDate  Guest arrival date
     * @param checkOutDate Guest departure date
     * @return PriceCalculationResponseDTO containing total price and night-by-night breakdown
     */
    @GetMapping("/calculate")
    public ResponseEntity<PriceCalculationResponseDTO> calculateStayPrice(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {

        PriceCalculationResponseDTO response = pricingEngineService.calculatePrice(roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get All Active Promotional/Seasonal Packages
     * Used by the front-end to showcase active discounts/promotions or by the Admin screen.
     *
     * Method: GET
     * URL Example: /api/pricing/packages
     */
    @GetMapping("/packages")
    public ResponseEntity<List<RateOverride>> getActivePackages() {
        List<RateOverride> activePackages = rateOverrideRepository.findByIsActiveTrue();
        return ResponseEntity.ok(activePackages);
    }

    /**
     * Admin - Create a New Rate Override / Promotional Package
     *
     * Method: POST
     * URL Example: /api/pricing/packages
     */
    @PostMapping("/packages")
    public ResponseEntity<RateOverride> createPackage(@RequestBody RateOverride rateOverride) {
        RateOverride savedPackage = rateOverrideRepository.save(rateOverride);
        return new ResponseEntity<>(savedPackage, HttpStatus.CREATED);
    }

    /**
     * For admin to deactivate a Package
     * Soft-deactivates a promotion without deleting historical audit records.
     *
     * Method: PATCH
     * URL Example: /api/pricing/packages/3/deactivate
     */
    @PatchMapping("/packages/{rateId}/deactivate")
    public ResponseEntity<Void> deactivatePackage(@PathVariable Integer rateId) {
        rateOverrideRepository.findById(rateId).ifPresent(pkg -> {
            pkg.setIsActive(false);
            rateOverrideRepository.save(pkg);
        });
        return ResponseEntity.noContent().build();
    }
}