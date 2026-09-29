package com.loft.hotel.controller;

import com.loft.hotel.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivityPageController {

    private final ActivityService activityService;

    @Autowired
    public ActivityPageController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/activities")
    public String showActivitiesPage(Model model) {
        model.addAttribute("activities", activityService.getAllActivities());
        return "activities";
        // Renders templates/activities.html
    }
}