package com.loft.hotel.repository;

import com.loft.hotel.model.MenuShowcase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuShowcaseRepository extends JpaRepository<MenuShowcase, Integer> {

    // JPQL: written against the Java class + field names (MenuShowcase, isAvailable),
    // NOT the table/column names (menu_showcase, is_available)
    @Query("SELECT m FROM MenuShowcase m WHERE m.isAvailable = true")
    List<MenuShowcase> findAvailableItems();
}
