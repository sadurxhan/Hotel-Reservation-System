package com.loft.hotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "room_pricing_tier")
public class RoomPricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Integer tierId;

    @Column(name = "tier_name", nullable = false, length = 50)
    private String tierName;

    @Column(name = "base_rate", nullable = false)
    private BigDecimal baseRate;

    public RoomPricingTier() {
    }

    public Integer getTierId() {
        return tierId;
    }
    public void setTierId(Integer tierId) {
        this.tierId = tierId;
    }

    public String getTierName() {
        return tierName;
    }
    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }
    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }
}
