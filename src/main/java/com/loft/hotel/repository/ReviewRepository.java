package com.loft.hotel.repository;

import com.loft.hotel.model.Review;
// This repository is for Review objects specifically.

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // Integer here matches Review's reviewId field type.
    // Same free methods as before, but now working on the "review" table.
}
