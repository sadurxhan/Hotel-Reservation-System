
package com.loft.hotel.service;

import com.loft.hotel.entity.Invoice;
import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.exception.ResourceNotFoundException;
import com.loft.hotel.repository.InvoiceRepository;
import com.loft.hotel.repository.PaymentRepository;
import com.loft.hotel.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MailService mailService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            MailService mailService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.mailService = mailService;
    }

    // Create an invoice once for a successful payment.
    @Transactional
    public Invoice createInvoice(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId
                ));

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Invoice can only be created for a PAID payment."
            );
        }

        return invoiceRepository.findByPaymentId(paymentId)
                .orElseGet(() -> invoiceRepository.save(
                        new Invoice(
                                UUID.randomUUID().toString(),
                                paymentId,
                                payment.getAmount(),
                                Invoice.InvoiceStatus.ISSUED
                        )
                ));
    }

    // Email the payment confirmation and invoice to the guest.
    // Returns true only if MailService reports success.
    @Transactional
    public boolean sendInvoice(String invoiceId) {
        Invoice invoice = getInvoice(invoiceId);

        Payment payment = paymentRepository.findById(invoice.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + invoice.getPaymentId()
                ));

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot send an invoice for an unpaid payment."
            );
        }

        Reservation reservation = reservationRepository.findById(
                payment.getReservationId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Reservation not found: " + payment.getReservationId()
        ));

        String body = "Hi " + reservation.getGuest().getFname() + ",\n\n"
                + "Your payment was successful and reservation #"
                + reservation.getReservationId() + " is confirmed.\n\n"
                + "Invoice: " + invoice.getInvoiceId() + "\n"
                + "Amount paid: LKR " + payment.getAmount() + "\n"
                + "Check-in: " + reservation.getCheckInDate() + "\n"
                + "Check-out: " + reservation.getCheckOutDate() + "\n\n"
                + "The Loft by the Lake";

        boolean sent = mailService.send(
                reservation.getGuest().getEmail(),
                "Payment confirmation & invoice",
                body
        );

        if (sent) {
            invoice.setInvoiceStatus(Invoice.InvoiceStatus.SENT);
            invoiceRepository.save(invoice);
        }

        return sent;
    }

    // Admin retry for an invoice whose email has not been sent.
    @Transactional
    public Invoice resendInvoice(String invoiceId) {
        Invoice invoice = getInvoice(invoiceId);

        if (invoice.getInvoiceStatus() != Invoice.InvoiceStatus.ISSUED) {
            throw new IllegalStateException(
                    "This invoice has already been sent."
            );
        }

        if (!sendInvoice(invoiceId)) {
            throw new IllegalStateException(
                    "Email could not be sent. Try again later."
            );
        }

        return getInvoice(invoiceId);
    }

    public List<Invoice> getUnsentInvoices() {
        return invoiceRepository.findByInvoiceStatus(
                Invoice.InvoiceStatus.ISSUED
        );
    }

    public Invoice getInvoice(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + invoiceId
                ));
    }

    public Invoice getInvoiceByPaymentId(String paymentId) {
        return invoiceRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found for payment: " + paymentId
                ));
    }
}
