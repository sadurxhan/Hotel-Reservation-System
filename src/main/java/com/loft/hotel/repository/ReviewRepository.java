package com.loft.hotel.repository;

import com.loft.hotel.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // used for APPROVED (public page) and PENDING (moderation queue)
    List<Review> findByReviewStatusOrderByReviewDateDesc(String reviewStatus);

    // all reviews, newest first (for the admin)
    List<Review> findAllByOrderByReviewDateDesc();
}
