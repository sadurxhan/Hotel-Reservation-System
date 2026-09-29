package com.loft.hotel.repository;

import com.loft.hotel.model.MenuShowcase;
// This one's for MenuShowcase objects (your menu display items).

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuShowcaseRepository extends JpaRepository<MenuShowcase, Integer> {
    // Integer matches menuId's type.
}
