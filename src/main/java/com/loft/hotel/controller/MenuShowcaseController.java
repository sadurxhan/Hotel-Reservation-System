package com.loft.hotel.controller;

import com.loft.hotel.model.MenuShowcase;
import com.loft.hotel.service.FileStorageService;
import com.loft.hotel.service.MenuShowcaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuShowcaseController {

    private final MenuShowcaseService menuShowcaseService;
    private final FileStorageService fileStorageService;

    public MenuShowcaseController(MenuShowcaseService menuShowcaseService,
                                  FileStorageService fileStorageService) {
        this.menuShowcaseService = menuShowcaseService;
        this.fileStorageService = fileStorageService;
    }

    // admin view: everything, including unavailable items
    @GetMapping("/all")
    public List<MenuShowcase> getAllMenuItems() {
        return menuShowcaseService.getAllMenuItems();
    }

    // fileUrl is now optional - the image is normally added with the upload endpoint below
    @PostMapping("/add")
    public MenuShowcase addMenuItem(@RequestParam String title,
                                    @RequestParam String mealType,
                                    @RequestParam String mealDescription,
                                    @RequestParam(required = false) String fileUrl) {
        return menuShowcaseService.addMenuItem(title, mealType, mealDescription, fileUrl);
    }

    @PutMapping("/update/{menuId}")
    public MenuShowcase updateMenuItem(@PathVariable Integer menuId,
                                       @RequestParam String title,
                                       @RequestParam String mealType,
                                       @RequestParam String mealDescription,
                                       @RequestParam(required = false) String fileUrl,
                                       @RequestParam Boolean isAvailable) {
        return menuShowcaseService.updateMenuItem(menuId, title, mealType,
                mealDescription, fileUrl, isAvailable);
    }

    // POST /api/menu/3/image   (form-data, key = "file")
    @PostMapping("/{menuId}/image")
    public MenuShowcase uploadImage(@PathVariable Integer menuId,
                                    @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "menu");   // 1. save the file, get its URL
        return menuShowcaseService.updateImageUrl(menuId, url); // 2. store the URL on the row
    }

    @DeleteMapping("/delete/{menuId}")
    public String deleteMenuItem(@PathVariable Integer menuId) {
        menuShowcaseService.deleteMenuItem(menuId);
        return "Menu item deleted successfully";
    }
}