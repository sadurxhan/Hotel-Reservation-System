package com.loft.hotel.controller;

import com.loft.hotel.dto.BookingRequest;
import com.loft.hotel.entity.Reservation;
import com.loft.hotel.service.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class BookingController {

    private final ReservationService service;

    public BookingController(ReservationService service) {
        this.service = service;
    }

    // Home page
    @GetMapping("/")
    public String home() {
        return "redirect:/book";
    }

    // Show booking form
    @GetMapping("/book")
    public String showForm(Model model) {
        model.addAttribute("bookingRequest", new BookingRequest());
        return "booking";
    }

    // Submit booking form
    @PostMapping("/book")
    public String submitBooking(
            @ModelAttribute BookingRequest request,
            Model model) {

        try {

            Reservation saved =
                    service.createReservation(request);

            return "redirect:/reservations/"
                    + saved.getReservationId();

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            model.addAttribute("bookingRequest", request);

            return "booking";
        }
    }

    // View reservation
    @GetMapping("/reservations/{id}")
    public String viewReservation(
            @PathVariable Integer id,
            Model model) {

        model.addAttribute(
                "r",
                service.getById(id)
        );

        return "reservation";
    }

    // Temporary payment simulation
    @PostMapping("/reservations/{id}/simulate-payment")
    public String simulatePayment(
            @PathVariable Integer id) {

        service.confirm(id);

        return "redirect:/reservations/" + id;
    }
}