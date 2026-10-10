package com.loft.hotel.service;

import com.loft.hotel.model.Review;
import com.loft.hotel.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final GuestLookupService guestLookupService;

    public ReviewService(ReviewRepository reviewRepository,
                         GuestLookupService guestLookupService) {
        this.reviewRepository = reviewRepository;
        this.guestLookupService = guestLookupService;
    }

    // ---------- Guest side ----------

    public Review submitReview(String fullName, String email, String phoneNo,
                               Integer rating, String comment) {

        if (isBlank(fullName) || isBlank(email) || isBlank(phoneNo)) {
            throw new IllegalArgumentException("Name, email and phone are required");
        }
        // The database also enforces this (chk_review_rating), but checking here
        // lets us give a friendly message instead of a database error
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Integer guestId = guestLookupService.findOrCreateGuest(fullName, email, phoneNo);

        Review review = new Review();
        review.setGuestId(guestId);
        review.setRating(rating);
        review.setReviewComment(comment);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewStatus("PENDING");     // hidden from the public until an admin approves it
        return reviewRepository.save(review);
    }

    // public Reviews page: only APPROVED ones
    public List<Review> getApprovedReviews() {
        return reviewRepository.findByReviewStatusOrderByReviewDateDesc("APPROVED");
    }

    // ---------- Admin side ----------

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByReviewDateDesc();
    }

    // the moderation queue
    public List<Review> getPendingReviews() {
        return reviewRepository.findByReviewStatusOrderByReviewDateDesc("PENDING");
    }

    public Review moderateReview(Integer reviewId, String newStatus) {
        String status = newStatus.toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setReviewStatus(status);
        return reviewRepository.save(review);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}