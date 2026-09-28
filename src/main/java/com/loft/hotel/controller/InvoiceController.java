package com.loft.hotel.controller;

import com.loft.hotel.entity.Invoice;
import com.loft.hotel.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // Create an invoice for a successful payment
    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestParam String paymentId) {
        Invoice invoice = invoiceService.createInvoice(paymentId);
        return ResponseEntity.ok(invoice);
    }

    // Get invoice by ID
    @GetMapping("/{invoiceId}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable String invoiceId) {
        Invoice invoice = invoiceService.getInvoice(invoiceId);
        return ResponseEntity.ok(invoice);
    }

    // Mark invoice as sent
    @PostMapping("/{invoiceId}/sent")
    public ResponseEntity<Invoice> markInvoiceAsSent(@PathVariable String invoiceId) {
        Invoice invoice = invoiceService.markInvoiceAsSent(invoiceId);
        return ResponseEntity.ok(invoice);
    }
}