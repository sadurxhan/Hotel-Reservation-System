package com.loft.hotel.controller;

import com.loft.hotel.entity.MenuShowcase;
import com.loft.hotel.service.FileStorageService;
import com.loft.hotel.service.MenuShowcaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/menu")   // every URL here starts with /admin/menu
public class MenuAdminPageController {

    private final MenuShowcaseService menuShowcaseService;
    private final FileStorageService fileStorageService;

    public MenuAdminPageController(MenuShowcaseService menuShowcaseService,
                                   FileStorageService fileStorageService) {
        this.menuShowcaseService = menuShowcaseService;
        this.fileStorageService = fileStorageService;
    }

    // GET /admin/menu -> the management page. The admin sees ALL meals,
    // including hidden ones (the public /menu page only shows available ones).
    @GetMapping
    public String showAdminPage(Model model) {
        return "redirect:/admin?tab=menu";
    }

    // POST /admin/menu/add -> add a new meal (photo is optional)
    @PostMapping("/add")
    public String addMenuItem(@RequestParam String title,
                              @RequestParam String mealType,
                              @RequestParam String mealDescription,
                              @RequestParam(value = "photo", required = false) MultipartFile photo) {
        // 1. save the meal first (it starts as available = true)
        MenuShowcase created = menuShowcaseService.addMenuItem(title, mealType, mealDescription, null);
        // 2. if a photo was chosen, save the file and put its URL on the new row
        if (photo != null && !photo.isEmpty()) {
            String url = fileStorageService.store(photo, "menu");
            menuShowcaseService.updateImageUrl(created.getMenuId(), url);
        }
        return "redirect:/admin/menu";   // reload the page with the new list
    }

    // POST /admin/menu/3/toggle -> hide/show on the public menu
    @PostMapping("/{menuId}/toggle")
    public String toggle(@PathVariable Integer menuId) {
        menuShowcaseService.toggleAvailable(menuId);
        return "redirect:/admin/menu";
    }

    // POST /admin/menu/3/image -> add or replace the photo
    @PostMapping("/{menuId}/image")
    public String uploadImage(@PathVariable Integer menuId,
                              @RequestParam("photo") MultipartFile photo) {
        String url = fileStorageService.store(photo, "menu");
        menuShowcaseService.updateImageUrl(menuId, url);
        return "redirect:/admin/menu";
    }

    // POST /admin/menu/3/delete -> remove it completely
    @PostMapping("/{menuId}/delete")
    public String delete(@PathVariable Integer menuId) {
        menuShowcaseService.deleteMenuItem(menuId);
        return "redirect:/admin/menu";
    }
}
