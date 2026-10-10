package com.loft.hotel.repository;

import com.loft.hotel.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {

    @Query("SELECT a FROM Activity a WHERE a.isActive = true")
    List<Activity> findActiveActivities();
}