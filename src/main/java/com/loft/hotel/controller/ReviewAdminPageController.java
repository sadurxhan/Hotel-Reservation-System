package com.loft.hotel.controller;

import com.loft.hotel.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/reviews")      // every URL in this class starts with /admin/reviews
public class ReviewAdminPageController {

    private final ReviewService reviewService;

    public ReviewAdminPageController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // GET /admin/reviews -> show the moderation page
    @GetMapping
    public String showModerationPage(Model model) {
        model.addAttribute("reviews", reviewService.getAllReviews());                  // everything, newest first
        model.addAttribute("pendingCount", reviewService.getPendingReviews().size());  // how many need action
        return "admin-reviews";                                                        // templates/admin-reviews.html
    }

    // POST /admin/reviews/3/moderate   with status=APPROVED or REJECTED
    @PostMapping("/{reviewId}/moderate")
    public String moderate(@PathVariable Integer reviewId, @RequestParam String status) {
        reviewService.moderateReview(reviewId, status);
        // "redirect:" tells the browser to load /admin/reviews again (fresh list),
        // instead of showing a page for this POST
        return "redirect:/admin/reviews";
    }
}
