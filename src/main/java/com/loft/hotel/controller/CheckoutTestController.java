
package com.loft.hotel.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
public class CheckoutTestController {

    @GetMapping("/dev/checkout-session")
    public String setCheckoutSession(
            @RequestParam Integer reservationId,
            HttpSession session) {

        session.setAttribute(
                PayHereController.CHECKOUT_SESSION_KEY,
                reservationId
        );

        return "Checkout session set for reservation " + reservationId
                + ". Now open /checkout?reservationId=" + reservationId;
    }
}
