package com.loft.hotel.controller;

import com.loft.hotel.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller   // returns page names (Thymeleaf), not JSON
public class ReviewPageController {

    private final ReviewService reviewService;

    public ReviewPageController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Public reviews page: shows only APPROVED reviews
    @GetMapping("/reviews")
    public String showReviews(Model model) {
        model.addAttribute("reviews", reviewService.getApprovedReviews());
        return "reviews";          // renders templates/reviews.html
    }

    // Runs when the guest submits the review form
    @PostMapping("/reviews")
    public String submitReview(@RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String phoneNo,
                               @RequestParam Integer rating,
                               @RequestParam String comment) {
        reviewService.submitReview(fullName, email, phoneNo, rating, comment);
        return "review-success";   // renders templates/review-success.html
    }
}
