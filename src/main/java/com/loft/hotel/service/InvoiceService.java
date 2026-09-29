package com.loft.hotel.service;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    // Spring injects both repositories
    public InvoiceService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    // Create an invoice for a successful payment
    @Transactional
    public Invoice createInvoice(String paymentId) {

        // Find the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found: " + paymentId
                        ));

        // Invoice can only be created for a PAID payment
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Invoice can only be created for a PAID payment."
            );
        }

        // Check whether an invoice already exists
        Optional<Invoice> existingInvoice =
                invoiceRepository.findByPaymentId(paymentId);

        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        // Generate invoice ID
        String invoiceId = UUID.randomUUID().toString();

        // Create invoice
        Invoice invoice = new Invoice(invoiceId, paymentId, payment.getAmount(), Invoice.InvoiceStatus.ISSUED);

        // Save invoice
        return invoiceRepository.save(invoice);
    }

    // Find invoice by invoice ID
    public Invoice getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invoice not found: " + invoiceId
                        ));
    }

    // Mark invoice as sent
    @Transactional
    public Invoice markInvoiceAsSent(String invoiceId) {
        Invoice invoice = getInvoice(invoiceId);

        // Only ISSUED invoices can be marked as SENT
        if (invoice.getInvoiceStatus() != Invoice.InvoiceStatus.ISSUED) {
            throw new IllegalStateException(
                    "Only ISSUED invoices can be marked as SENT."
            );
        }

        invoice.setInvoiceStatus(Invoice.InvoiceStatus.SENT);
        return invoiceRepository.save(invoice);
    }

    public Invoice getInvoiceByPaymentId(String paymentId) {
        return invoiceRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invoice not found for payment: " + paymentId
                        ));
    }
}