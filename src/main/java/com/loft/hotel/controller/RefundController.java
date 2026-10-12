package com.loft.hotel.controller;

import com.loft.hotel.entity.Refund;
import com.loft.hotel.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Refunds are created automatically on cancellation. Everything here is admin-only.
@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;
    private final AdminGuard adminGuard;

    public RefundController(RefundService refundService, AdminGuard adminGuard) {
        this.refundService = refundService;
        this.adminGuard = adminGuard;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Refund>> pending(@RequestHeader("X-Admin-Key") String key) {
        adminGuard.check(key);
        return ResponseEntity.ok(refundService.getPendingRefunds());
    }

    @GetMapping
    public ResponseEntity<Refund> byReservation(@RequestHeader("X-Admin-Key") String key,
                                                @RequestParam Integer reservationId) {
        adminGuard.check(key);
        return ResponseEntity.ok(refundService.getRefundByReservationId(reservationId));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<Refund> get(@RequestHeader("X-Admin-Key") String key, @PathVariable String refundId) {
        adminGuard.check(key);
        return ResponseEntity.ok(refundService.getRefund(refundId));
    }

    // Admin paid the guest manually, now mark it as paid
    @PostMapping("/{refundId}/paid")
    public ResponseEntity<Refund> paid(@RequestHeader("X-Admin-Key") String key, @PathVariable String refundId) {
        adminGuard.check(key);
        return ResponseEntity.ok(refundService.markRefundAsPaid(refundId));
    }
}