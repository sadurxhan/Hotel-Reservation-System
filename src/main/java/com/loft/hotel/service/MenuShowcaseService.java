package com.loft.hotel.service;

import com.loft.hotel.model.MenuShowcase;
import com.loft.hotel.repository.MenuShowcaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuShowcaseService {

    private final MenuShowcaseRepository menuShowcaseRepository;

    @Autowired
    public MenuShowcaseService(MenuShowcaseRepository menuShowcaseRepository) {
        this.menuShowcaseRepository = menuShowcaseRepository;
    }

    public List<MenuShowcase> getAllMenuItems() {
        // Used by the public-facing page guests browsing the menu.
        return menuShowcaseRepository.findAll();
    }

    public MenuShowcase addMenuItem(String title, String mealType,
                                    String mealDescription, String fileUrl) {
        // Used by the admin side like adding a new menu card.
        MenuShowcase item = new MenuShowcase();
        item.setTitle(title);
        item.setMealType(mealType);
        item.setMealDescription(mealDescription);
        item.setFileUrl(fileUrl);
        item.setAvailable(true);
        // New items are visible to guests by default when added.

        return menuShowcaseRepository.save(item);
    }

    public MenuShowcase updateMenuItem(Integer menuId, String title, String mealType,
                                       String mealDescription, String fileUrl,
                                       Boolean isAvailable) {
        // Used by admin — editing an existing item.
        MenuShowcase item = menuShowcaseRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        item.setTitle(title);
        item.setMealType(mealType);
        item.setMealDescription(mealDescription);
        item.setFileUrl(fileUrl);
        item.setAvailable(isAvailable);

        return menuShowcaseRepository.save(item);
    }

    public void deleteMenuItem(Integer menuId) {
        // Used by admin removing an item entirely.
        menuShowcaseRepository.deleteById(menuId);
        // deleteById() is another free method from JpaRepository.
    }
}