package com.loft.hotel.controller;

import com.loft.hotel.model.Activity;
import com.loft.hotel.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    //automatically hand spring a working ActivityServic when this class is created
    @Autowired
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    //when visits this url in the browser
    @GetMapping("/all")
    public List<Activity> getAllActivities() {
        return activityService.getAllActivities();
    }

    //request sends new data to be created
    @PostMapping("/add")
    public Activity addActivity(@RequestParam String title,
                                @RequestParam String activityDescription,
                                @RequestParam String pricingNote,
                                @RequestParam String imageUrl) {
        return activityService.addActivity(title, activityDescription, pricingNote, imageUrl);
    }

    //when the data should be update
    @PutMapping("/update/{activityId}")
    public Activity updateActivity(@PathVariable Integer activityId,
                                   @RequestParam String title,
                                   @RequestParam String activityDescription,
                                   @RequestParam String pricingNote,
                                   @RequestParam String imageUrl,
                                   @RequestParam Boolean isActive) {
        return activityService.updateActivity(activityId, title, activityDescription,
                pricingNote, imageUrl, isActive);
    }

    //when something should be removed
    @DeleteMapping("/delete/{activityId}")
    public String deleteActivity(@PathVariable Integer activityId) {
        activityService.deleteActivity(activityId);
        return "Activity deleted successfully"; //sending back confirmation msg
    }
}
