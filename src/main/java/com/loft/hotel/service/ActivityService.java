package com.loft.hotel.service;

import com.loft.hotel.model.Activity;
import com.loft.hotel.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Autowired
    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public List<Activity> getAllActivities() {
        // Used by the public-facing page — guests browsing activities.
        return activityRepository.findAll();
    }

    public Activity addActivity(String title, String activityDescription,
                                String pricingNote, String imageUrl) {
        // Used by the admin side — adding a new activity card.
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setActivityDescription(activityDescription);
        activity.setPricingNote(pricingNote);
        activity.setImageUrl(imageUrl);
        activity.setActive(true);

        return activityRepository.save(activity);
    }

    public Activity updateActivity(Integer activityId, String title, String activityDescription,
                                   String pricingNote, String imageUrl, Boolean isActive) {
        // Used by admin — editing an existing activity.
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        activity.setTitle(title);
        activity.setActivityDescription(activityDescription);
        activity.setPricingNote(pricingNote);
        activity.setImageUrl(imageUrl);
        activity.setActive(isActive);

        return activityRepository.save(activity);
    }

    public void deleteActivity(Integer activityId) {
        // Used by admin — removing an activity entirely.
        activityRepository.deleteById(activityId);
    }
}