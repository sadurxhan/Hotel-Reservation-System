package com.loft.hotel.controller;

import com.loft.hotel.service.MenuShowcaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
// @Controller (not @RestController) — this one returns page NAMES,
// and Spring finds the matching HTML file in /templates/ automatically.
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuPageController {

    private final MenuShowcaseService menuShowcaseService;

    @Autowired
    public MenuPageController(MenuShowcaseService menuShowcaseService) {
        this.menuShowcaseService = menuShowcaseService;
    }

    @GetMapping("/menu")
    // Visiting that localhost in a browser triggers this.
    public String showMenuPage(Model model) {
        // hand data over to the HTML page.
        model.addAttribute("menuItems", menuShowcaseService.getAllMenuItems());
        // "menuItems" is the name the HTML page

        return "menu";

    }
}