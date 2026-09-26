package com.loft.hotel.repository;

import com.loft.hotel.model.Activity;
// This one's for Activity objects (kayaking, cycling, etc.).

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    // Integer matches activityId's type.
}