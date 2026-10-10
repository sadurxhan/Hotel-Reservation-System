package com.loft.hotel.controller;

import com.loft.hotel.entity.Cancellation;
import com.loft.hotel.service.CancellationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cancellations")
public class CancellationController {

    private final CancellationService cancellationService;
    private final AdminGuard adminGuard;

    public CancellationController(CancellationService cancellationService, AdminGuard adminGuard) {
        this.cancellationService = cancellationService;
        this.adminGuard = adminGuard;
    }

    // ----- Guest -----
    // 201 when a request was accepted for admin review (REQUESTED).
    // 200 when it was auto-rejected (check-in within 2 days); the body shows status REJECTED.
    @PostMapping
    public ResponseEntity<Cancellation> request(@RequestParam Integer reservationId,
                                                @RequestParam String email,
                                                @RequestParam String reason) {
        Cancellation c = cancellationService.requestCancellation(reservationId, email, reason);
        HttpStatus status = c.getCancellationStatus() == Cancellation.CancellationStatus.REQUESTED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(c);
    }

    // ----- Admin -----
    // e.g. GET /api/cancellations?status=REQUESTED  (omit status to list all)
    @GetMapping
    public ResponseEntity<List<Cancellation>> list(@RequestHeader("X-Admin-Key") String key,
                                                   @RequestParam(required = false) Cancellation.CancellationStatus status) {
        adminGuard.check(key);
        return ResponseEntity.ok(cancellationService.getCancellations(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cancellation> get(@RequestHeader("X-Admin-Key") String key, @PathVariable String id) {
        adminGuard.check(key);
        return ResponseEntity.ok(cancellationService.getCancellation(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Cancellation> approve(@RequestHeader("X-Admin-Key") String key, @PathVariable String id) {
        adminGuard.check(key);
        return ResponseEntity.ok(cancellationService.approveCancellation(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Cancellation> reject(@RequestHeader("X-Admin-Key") String key, @PathVariable String id) {
        adminGuard.check(key);
        return ResponseEntity.ok(cancellationService.rejectCancellation(id));
    }

    @PostMapping("/admin-cancel")
    public ResponseEntity<Cancellation> adminCancel(@RequestHeader("X-Admin-Key") String key,
                                                    @RequestParam Integer reservationId,
                                                    @RequestParam String reason) {
        adminGuard.check(key);
        return ResponseEntity.ok(cancellationService.adminCancel(reservationId, reason));
    }
}