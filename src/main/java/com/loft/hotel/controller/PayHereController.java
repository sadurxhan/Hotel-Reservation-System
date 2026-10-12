package com.loft.hotel.controller;

import com.loft.hotel.entity.Guest;
import com.loft.hotel.entity.Invoice;
import com.loft.hotel.entity.Payment;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.exception.ResourceNotFoundException;
import com.loft.hotel.repository.ReservationRepository;
import com.loft.hotel.service.InvoiceService;
import com.loft.hotel.service.PayHereService;
import com.loft.hotel.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class PayHereController {

    private static final Logger log = LoggerFactory.getLogger(PayHereController.class);

    // The booking flow must set this right after it creates the reservation:
    //   session.setAttribute(PayHereController.CHECKOUT_SESSION_KEY, reservation.getReservationId());
    public static final String CHECKOUT_SESSION_KEY = "checkoutReservationId";

    private final PayHereService payHere;
    private final PaymentService paymentService;
    private final ReservationRepository reservationRepository;
    private final InvoiceService invoiceService;

    // Public URL of your app (use ngrok in dev - PayHere can't reach localhost)
    @Value("${app.base-url}")
    private String baseUrl;

    public PayHereController(PayHereService payHere,
                             PaymentService paymentService,
                             ReservationRepository reservationRepository,
                             InvoiceService invoiceService) {
        this.payHere = payHere;
        this.paymentService = paymentService;
        this.reservationRepository = reservationRepository;
        this.invoiceService = invoiceService;
    }

    // 1) Booking done -> guest lands here. Shows summary + "Pay Now".
    @GetMapping("/checkout")
    public String checkout(@RequestParam Integer reservationId, HttpSession session, Model model) {

        // Reservation IDs are sequential, so only the browser session that just made
        // this booking may open its checkout (the page contains guest email/phone).
        // Accepts any Number type (Integer or Long) stored by the booking flow.
        Object sessionValue = session.getAttribute(CHECKOUT_SESSION_KEY);
        if (!(sessionValue instanceof Number n) || n.intValue() != reservationId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Please start from your booking to pay for this reservation.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationId));

        // Creates a PENDING payment (or reuses the existing one)
        Payment payment = paymentService.createPayment(reservationId, Payment.PaymentMethod.Credit_Card);

        String amount = payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        Guest guest = reservation.getGuest();

        model.addAttribute("reservation", reservation);
        model.addAttribute("amount", amount);
        model.addAttribute("payhereUrl", payHere.getCheckoutUrl());
        model.addAttribute("merchantId", payHere.getMerchantId());
        model.addAttribute("orderId", payment.getPaymentId());
        model.addAttribute("currency", "LKR");
        model.addAttribute("hash", payHere.checkoutHash(payment.getPaymentId(), amount, "LKR"));
        model.addAttribute("returnUrl", baseUrl + "/payment/result?paymentId=" + payment.getPaymentId());
        model.addAttribute("cancelUrl", baseUrl + "/checkout?reservationId=" + reservationId);
        model.addAttribute("notifyUrl", baseUrl + "/payhere/notify");
        model.addAttribute("guest", guest);
        return "billing/checkout";
    }

    // 2) PayHere calls this server-to-server after the payment
    @PostMapping("/payhere/notify")
    @ResponseBody
    public String payhereNotify(@RequestParam("merchant_id") String merchantId,
                                @RequestParam("order_id") String orderId,
                                @RequestParam("payment_id") String payhereId,
                                @RequestParam("payhere_amount") String amount,
                                @RequestParam("payhere_currency") String currency,
                                @RequestParam("status_code") String statusCode,
                                @RequestParam("md5sig") String md5sig) {

        // Reject anything that isn't genuinely from PayHere
        if (!payHere.verifyNotify(merchantId, orderId, amount, currency, statusCode, md5sig)) {
            log.warn("Rejected PayHere notification with invalid signature for order {}", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid PayHere notification signature.");
        }
        if (!"LKR".equals(currency)) {
            log.warn("Rejected PayHere notification with unexpected currency {} for order {}",
                    currency, orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected payment currency.");
        }

        switch (statusCode) {
            case "2" -> {
                // Success: verify and record the payment first.
                try {
                    paymentService.markPaymentAsPaid(orderId, payhereId, new BigDecimal(amount));
                } catch (IllegalStateException | ResourceNotFoundException e) {
                    // Genuine PayHere payment that we could NOT record (cancelled reservation,
                    // mismatch, unknown order). Money was taken, so keep evidence for manual
                    // reconciliation. Answer OK because retrying cannot fix this.
                    log.error("Verified PayHere payment {} for order {} could not be recorded. "
                                    + "Amount: {} LKR. Manual reconciliation required: {}",
                            payhereId, orderId, amount, e.getMessage());
                    return "OK";
                }

                // Email failure must not be treated as a payment failure.
                try {
                    Invoice invoice = invoiceService.getInvoiceByPaymentId(orderId);
                    // Send automatically only while the invoice is unsent (skips duplicate notifies)
                    if (invoice.getInvoiceStatus() == Invoice.InvoiceStatus.ISSUED
                            && !invoiceService.sendInvoice(invoice.getInvoiceId())) {
                        log.warn("Payment {} recorded, but invoice {} email failed. Use admin resend.",
                                orderId, invoice.getInvoiceId());
                    }
                } catch (Exception e) {
                    log.error("Payment {} recorded, but invoice email processing failed.", orderId, e);
                }
            }

            case "-2" -> { // failed
                try {
                    Payment payment = paymentService.getPayment(orderId);
                    if (payment.getPaymentStatus() == Payment.PaymentStatus.PENDING) {
                        paymentService.markPaymentAsFailed(orderId);
                    }
                } catch (ResourceNotFoundException | IllegalStateException e) {
                    // Unknown order, or the payment changed state at the same moment: nothing to retry
                    log.warn("PayHere failure notification for order {} not applied: {}",
                            orderId, e.getMessage());
                }
            }

            case "-3" -> // chargeback: don't change anything automatically, flag for the admin
                    log.error("PayHere CHARGEBACK for order {} (PayHere payment {}). Admin review required.",
                            orderId, payhereId);

            default -> // 0 = pending, -1 = cancelled: leave PENDING so the guest can retry
                    log.info("PayHere notification for order {} has status {}", orderId, statusCode);
        }
        return "OK";
    }
}