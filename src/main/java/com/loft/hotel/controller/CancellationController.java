package com.loft.hotel.controller;

import com.loft.hotel.entity.Cancellation;
import com.loft.hotel.service.CancellationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cancellations")
public class CancellationController {
    private final CancellationService cancellationService;

    public CancellationController(CancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    // Create a cancellation request
    @PostMapping
    public ResponseEntity<Cancellation> createCancellation(@RequestParam Integer reservationId, @RequestParam Cancellation.RequestedBy requestedBy, @RequestParam String cancellationReason) {
        Cancellation cancellation = cancellationService.createCancellation(reservationId, requestedBy, cancellationReason);
        return ResponseEntity.ok(cancellation);
    }

    // Get cancellation by ID
    @GetMapping("/{cancellationId}")
    public ResponseEntity<Cancellation> getCancellation(@PathVariable String cancellationId) {
        Cancellation cancellation = cancellationService.getCancellation(cancellationId);
        return ResponseEntity.ok(cancellation);
    }

    // Approve cancellation
    @PostMapping("/{cancellationId}/approve")
    public ResponseEntity<Cancellation> approveCancellation(@PathVariable String cancellationId) {
        Cancellation cancellation = cancellationService.approveCancellation(cancellationId);
        return ResponseEntity.ok(cancellation);
    }

    // Reject cancellation
    @PostMapping("/{cancellationId}/reject")
    public ResponseEntity<Cancellation> rejectCancellation(@PathVariable String cancellationId) {
        Cancellation cancellation = cancellationService.rejectCancellation(cancellationId);
        return ResponseEntity.ok(cancellation);
    }
}