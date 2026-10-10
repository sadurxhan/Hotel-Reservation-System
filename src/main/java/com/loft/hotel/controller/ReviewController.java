package com.loft.hotel.controller;

import com.loft.hotel.model.Review;
import com.loft.hotel.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/all")
    public List<Review> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @PutMapping("/moderate/{reviewId}")
    public Review moderateReview(@PathVariable Integer reviewId,
                                 @RequestParam String status) {
        return reviewService.moderateReview(reviewId, status);
    }
    // GET /api/review/pending -> the moderation queue
    @GetMapping("/pending")
    public List<Review> getPendingReviews() {
        return reviewService.getPendingReviews();
    }
}
