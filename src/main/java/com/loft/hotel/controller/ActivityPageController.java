package com.loft.hotel.controller;

import com.loft.hotel.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivityPageController {

    private final ActivityService activityService;

    public ActivityPageController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/activities")
    public String showActivitiesPage(Model model) {
        // changed: guests now only see activities where is_active = true
        model.addAttribute("activities", activityService.getActiveActivities());
        return "activities";
    }
}