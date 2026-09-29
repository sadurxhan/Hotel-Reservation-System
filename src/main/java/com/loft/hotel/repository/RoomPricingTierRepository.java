package com.loft.hotel.repository;

import com.loft.hotel.entity.RoomPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPricingTierRepository extends JpaRepository<RoomPricingTier, Integer> {
}
