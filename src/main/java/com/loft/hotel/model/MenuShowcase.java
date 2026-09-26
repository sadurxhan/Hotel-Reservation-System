package com.loft.hotel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_showcase")
public class MenuShowcase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "meal_type", nullable = false, length = 50)
    private String mealType;

    @Column(name = "meal_description", columnDefinition = "TEXT")
    private String mealDescription;

    @Column(name = "file_url", length = 300)
    private String fileUrl;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    public Integer getMenuId() {
        return menuId;
    }

    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getMealDescription() {
        return mealDescription;
    }

    public void setMealDescription(String mealDescription) {
        this.mealDescription = mealDescription;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }


}