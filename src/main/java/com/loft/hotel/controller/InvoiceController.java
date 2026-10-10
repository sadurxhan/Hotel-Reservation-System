
package com.loft.hotel.controller;

import com.loft.hotel.entity.Invoice;
import com.loft.hotel.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All invoice APIs are admin-only.
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AdminGuard adminGuard;

    public InvoiceController(
            InvoiceService invoiceService,
            AdminGuard adminGuard
    ) {
        this.invoiceService = invoiceService;
        this.adminGuard = adminGuard;
    }

    // Admin: get invoice by invoice ID.
    @GetMapping("/{invoiceId}")
    public ResponseEntity<Invoice> getInvoice(
            @RequestHeader("X-Admin-Key") String key,
            @PathVariable String invoiceId
    ) {
        adminGuard.check(key);
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId));
    }

    // Admin: get invoice by payment ID.
    @GetMapping
    public ResponseEntity<Invoice> getByPayment(
            @RequestHeader("X-Admin-Key") String key,
            @RequestParam String paymentId
    ) {
        adminGuard.check(key);
        return ResponseEntity.ok(
                invoiceService.getInvoiceByPaymentId(paymentId)
        );
    }

    // Admin: list invoices whose email has not been sent.
    @GetMapping("/unsent")
    public ResponseEntity<List<Invoice>> unsent(
            @RequestHeader("X-Admin-Key") String key
    ) {
        adminGuard.check(key);
        return ResponseEntity.ok(invoiceService.getUnsentInvoices());
    }

    // Admin: retry sending an invoice email.
    @PostMapping("/{invoiceId}/resend")
    public ResponseEntity<Invoice> resend(
            @RequestHeader("X-Admin-Key") String key,
            @PathVariable String invoiceId
    ) {
        adminGuard.check(key);
        return ResponseEntity.ok(invoiceService.resendInvoice(invoiceId));
    }
}
