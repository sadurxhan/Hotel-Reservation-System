package com.loft.hotel.controller;

import com.loft.hotel.model.Activity;
import com.loft.hotel.service.ActivityService;
import com.loft.hotel.service.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/activities")   // every URL here starts with /admin/activities
public class ActivityAdminPageController {

    private final ActivityService activityService;
    private final FileStorageService fileStorageService;

    public ActivityAdminPageController(ActivityService activityService,
                                       FileStorageService fileStorageService) {
        this.activityService = activityService;
        this.fileStorageService = fileStorageService;
    }

    // GET /admin/activities -> the management page. The admin sees ALL activities,
    // including hidden ones (the public page only shows the active ones).
    @GetMapping
    public String showAdminPage(Model model) {
        model.addAttribute("activities", activityService.getAllActivities());
        return "admin-activities";     // templates/admin-activities.html
    }

    // POST /admin/activities/add -> add a new activity (photo is optional)
    @PostMapping("/add")
    public String addActivity(@RequestParam String title,
                              @RequestParam String activityDescription,
                              @RequestParam String pricingNote,
                              @RequestParam(value = "photo", required = false) MultipartFile photo) {
        // 1. save the activity first (it starts as active = true)
        Activity created = activityService.addActivity(title, activityDescription, pricingNote, null);
        // 2. if a photo was chosen, save the file and put its URL on the new row
        if (photo != null && !photo.isEmpty()) {
            String url = fileStorageService.store(photo, "activities");
            activityService.updateImageUrl(created.getActivityId(), url);
        }
        return "redirect:/admin/activities";   // reload the page with the new list
    }

    // POST /admin/activities/3/toggle -> hide/show on the public site
    @PostMapping("/{activityId}/toggle")
    public String toggle(@PathVariable Integer activityId) {
        activityService.toggleActive(activityId);
        return "redirect:/admin/activities";
    }

    // POST /admin/activities/3/image -> add or replace the photo
    @PostMapping("/{activityId}/image")
    public String uploadImage(@PathVariable Integer activityId,
                              @RequestParam("photo") MultipartFile photo) {
        String url = fileStorageService.store(photo, "activities");
        activityService.updateImageUrl(activityId, url);
        return "redirect:/admin/activities";
    }

    // POST /admin/activities/3/delete -> remove it completely
    @PostMapping("/{activityId}/delete")
    public String delete(@PathVariable Integer activityId) {
        activityService.deleteActivity(activityId);
        return "redirect:/admin/activities";
    }
}