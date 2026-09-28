package com.loft.hotel.controller;

import com.loft.hotel.entity.Refund;
import com.loft.hotel.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    // Create a refund request for an approved cancellation
    @PostMapping
    public ResponseEntity<Refund> createRefund(@RequestParam String paymentId, @RequestParam String cancellationId) {
        Refund refund = refundService.createRefund(paymentId, cancellationId);
        return ResponseEntity.ok(refund);
    }

    // Get refund by ID
    @GetMapping("/{refundId}")
    public ResponseEntity<Refund> getRefund(@PathVariable String refundId) {
        Refund refund = refundService.getRefund(refundId);
        return ResponseEntity.ok(refund);
    }

    // Approve a pending refund
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<Refund> approveRefund(@PathVariable String refundId) {
        Refund refund = refundService.approveRefund(refundId);
        return ResponseEntity.ok(refund);
    }

    // Mark an approved refund as actually refunded
    @PostMapping("/{refundId}/refunded")
    public ResponseEntity<Refund> markRefundAsRefunded(@PathVariable String refundId) {
        Refund refund = refundService.markRefundAsRefunded(refundId);
        return ResponseEntity.ok(refund);
    }

    // Reject a pending refund
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<Refund> rejectRefund(@PathVariable String refundId) {
        Refund refund = refundService.rejectRefund(refundId);
        return ResponseEntity.ok(refund);
    }
}