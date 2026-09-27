package com.loft.hotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarViewController {

    @GetMapping("/calendar")
    public String viewCalendar() {
        return "calendar"; // Loads src/main/resources/templates/calendar.html
    }
}