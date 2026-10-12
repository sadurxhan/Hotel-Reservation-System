package com.loft.hotel.controller;

import com.loft.hotel.service.MenuShowcaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuPageController {

    private final MenuShowcaseService menuShowcaseService;

    public MenuPageController(MenuShowcaseService menuShowcaseService) {
        this.menuShowcaseService = menuShowcaseService;
    }

    @GetMapping("/menu")
    public String showMenuPage(Model model) {
        // changed: guests now only see meals where is_available = true
        model.addAttribute("menuItems", menuShowcaseService.getAvailableMenuItems());
        return "menu";
    }
}
