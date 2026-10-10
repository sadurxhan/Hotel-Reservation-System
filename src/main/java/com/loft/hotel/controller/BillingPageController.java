package com.loft.hotel.controller;

import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.exception.ResourceNotFoundException;
import com.loft.hotel.repository.ReservationRepository;
import com.loft.hotel.service.InvoiceService;
import com.loft.hotel.service.PaymentService;
import com.loft.hotel.service.RefundService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class BillingPageController {

    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final RefundService refundService;

    public BillingPageController(ReservationRepository reservationRepository,
                                 PaymentService paymentService,
                                 InvoiceService invoiceService,
                                 RefundService refundService) {
        this.reservationRepository = reservationRepository;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.refundService = refundService;
    }

    // Old link kept: redirects to the PayHere checkout page
    @GetMapping("/payment")
    public String paymentPage(@RequestParam Integer reservationId) {
        return "redirect:/checkout?reservationId=" + reservationId;
    }

    // PayHere return_url lands here. The notify may arrive a moment later,
    // so the template should show "Processing..." while status is PENDING.
    @GetMapping("/payment/result")
    public String paymentResultPage(@RequestParam String paymentId, Model model) {
        Payment payment = paymentService.getPayment(paymentId);
        model.addAttribute("payment", payment);
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            model.addAttribute("invoice", invoiceService.getInvoiceByPaymentId(paymentId));
        }
        return "billing/payment-result";
    }

    // Public page: reservation IDs are sequential, so only non-personal booking details
    // are exposed. The guest proves identity with their email when submitting the form.
    @GetMapping("/cancellation")
    public String cancellationPage(@RequestParam Integer reservationId, Model model) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationId));

        model.addAttribute("booking", new BookingSummary(
                reservation.getReservationId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getTotalAmount()));
        return "billing/cancellation";
    }

    @GetMapping("/refund")
    public String refundPage(@RequestParam String refundId, Model model) {
        model.addAttribute("refund", refundService.getRefund(refundId));
        return "billing/refund";
    }

    @GetMapping("/invoice")
    public String invoicePage(@RequestParam String invoiceId, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoice(invoiceId));
        return "billing/invoice";
    }

    // Placeholder: point this at your booking / home page once it exists.
    @GetMapping("/")
    public String home() {
        return "redirect:/checkout?reservationId=1";
    }

    // Only the fields the cancellation form needs (no guest name, email or phone).
    public record BookingSummary(Integer reservationId,
                                 LocalDate checkInDate,
                                 LocalDate checkOutDate,
                                 BigDecimal totalAmount) {
    }
}