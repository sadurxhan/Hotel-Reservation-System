package com.loft.hotel.controller;

import com.loft.hotel.entity.Invoice;
import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Refund;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.repository.ReservationRepository;
import com.loft.hotel.service.InvoiceService;
import com.loft.hotel.service.PaymentService;
import com.loft.hotel.service.RefundService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BillingPageController {
    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final InvoiceService invoiceService;

    public BillingPageController(ReservationRepository reservationRepository, PaymentService paymentService, RefundService refundService, InvoiceService invoiceService) {
        this.reservationRepository = reservationRepository;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/payment")
    public String paymentPage(
            @RequestParam Integer reservationId,
            Model model) {

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + reservationId
                                ));

        model.addAttribute("reservation", reservation);
        return "billing/payment";
    }

    @GetMapping("/payment/result")
    public String paymentResultPage(@RequestParam String paymentId, Model model) {
        Payment payment = paymentService.getPayment(paymentId);

        model.addAttribute("payment", payment);

        /*
         * Invoice is automatically created when
         * the payment becomes PAID.
         */
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PAID) {
            Invoice invoice = invoiceService.getInvoiceByPaymentId(paymentId);
            model.addAttribute("invoice", invoice);
        }
        return "billing/payment-result";
    }

    @GetMapping("/cancellation")
    public String cancellationPage(@RequestParam Integer reservationId, Model model) {

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reservation not found: "
                                                + reservationId
                                ));

        model.addAttribute("reservation", reservation);
        return "billing/cancellation";
    }

    @GetMapping("/refund")
    public String refundPage(@RequestParam String refundId, Model model) {
        Refund refund = refundService.getRefund(refundId);
        model.addAttribute("refund", refund);
        return "billing/refund";
    }

    @GetMapping("/invoice")
    public String invoicePage(@RequestParam String invoiceId, Model model) {
        Invoice invoice = invoiceService.getInvoice(invoiceId);
        model.addAttribute("invoice", invoice);
        return "billing/invoice";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/payment?reservationId=1";
    }
}