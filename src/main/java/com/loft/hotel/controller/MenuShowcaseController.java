package com.loft.hotel.controller;
// Controllers live in their own "controller" package — this is the
// layer that listens for web requests and decides what to do with them.

import com.loft.hotel.model.MenuShowcase;
import com.loft.hotel.service.MenuShowcaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// This tells Spring: "this class handles web requests, and whatever
// each method returns should be sent back as the response directly
// (in this case, as JSON — a text format browsers/apps can read)."

@RequestMapping("/api/menu")
// Every URL in this class starts with /api/menu — e.g. a method
// mapped to "/all" below actually becomes /api/menu/all.

public class MenuShowcaseController {

    private final MenuShowcaseService menuShowcaseService;

    @Autowired
    public MenuShowcaseController(MenuShowcaseService menuShowcaseService) {
        this.menuShowcaseService = menuShowcaseService;
    }

    @GetMapping("/all")
    // @GetMapping means: this method runs when someone visits this URL
    // in a browser, or a GET request hits it (GET = "just show me data").
    public List<MenuShowcase> getAllMenuItems() {
        return menuShowcaseService.getAllMenuItems();
        // Whatever this returns gets automatically converted to JSON
        // and sent back — Spring handles that conversion for us.
    }

    @PostMapping("/add")
    // @PostMapping means: this runs when a form/request SENDS new data
    // to be created (POST = "here's something new, save it").
    public MenuShowcase addMenuItem(@RequestParam String title,
                                    @RequestParam String mealType,
                                    @RequestParam String mealDescription,
                                    @RequestParam String fileUrl) {
        // @RequestParam pulls each value out of the incoming form data
        // by matching these parameter names.
        return menuShowcaseService.addMenuItem(title, mealType, mealDescription, fileUrl);
    }

    @PutMapping("/update/{menuId}")
    // @PutMapping means: this runs when existing data should be UPDATED.
    // {menuId} in the URL is a placeholder — e.g. /api/menu/update/3
    public MenuShowcase updateMenuItem(@PathVariable Integer menuId,
                                       @RequestParam String title,
                                       @RequestParam String mealType,
                                       @RequestParam String mealDescription,
                                       @RequestParam String fileUrl,
                                       @RequestParam Boolean isAvailable) {
        // @PathVariable grabs the {menuId} straight out of the URL itself.
        return menuShowcaseService.updateMenuItem(menuId, title, mealType,
                mealDescription, fileUrl, isAvailable);
    }

    @DeleteMapping("/delete/{menuId}")
    // @DeleteMapping means: this runs when something should be REMOVED.
    public String deleteMenuItem(@PathVariable Integer menuId) {
        menuShowcaseService.deleteMenuItem(menuId);
        return "Menu item deleted successfully";
        // Just sending back a simple confirmation message.
    }
}
