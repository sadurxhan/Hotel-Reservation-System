package com.loft.hotel.service;

import com.loft.hotel.model.Review;
import com.loft.hotel.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, JdbcTemplate jdbcTemplate) {
        this.reviewRepository = reviewRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Integer findOrCreateGuest(String fname, String lname, String email, String phoneNo) {
        // Same lookup-or-create pattern as InquiryService.
        List<Integer> existing = jdbcTemplate.queryForList(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, email
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO guest (fname, lname, email, phone_no) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, fname);
            ps.setString(2, lname);
            ps.setString(3, email);
            ps.setString(4, phoneNo);
            return ps;
        });

        return jdbcTemplate.queryForObject(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, email
        );
    }

    public Review submitReview(String fullName, String email, String phoneNo,
                               Integer rating, String comment) {
        String[] nameParts = fullName.trim().split(" ", 2);
        String fname = nameParts[0];
        String lname = nameParts.length > 1 ? nameParts[1] : "";

        Integer guestId = findOrCreateGuest(fname, lname, email, phoneNo);

        Review review = new Review();
        review.setGuestId(guestId);
        review.setRating(rating);
        review.setReviewComment(comment);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewStatus("PENDING");
        // Every new review starts PENDING until an admin approves it.

        return reviewRepository.save(review);
    }

    public List<Review> getAllReviews() {
        // Admin side — sees everything, including PENDING.
        return reviewRepository.findAll();
    }

    public List<Review> getApprovedReviews() {
        // Public side — guests only see APPROVED reviews.
        return reviewRepository.findAll().stream()
                .filter(r -> "APPROVED".equals(r.getReviewStatus()))
                .toList();
    }

    public Review moderateReview(Integer reviewId, String newStatus) {
        // newStatus will be "APPROVED" or "REJECTED"
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setReviewStatus(newStatus);
        return reviewRepository.save(review);
    }
}
