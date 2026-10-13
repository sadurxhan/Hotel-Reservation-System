package com.loft.hotel.config;

import com.loft.hotel.entity.Room;
import com.loft.hotel.entity.RoomPricingTier;
import com.loft.hotel.repository.RoomPricingTierRepository;
import com.loft.hotel.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Runs once automatically at startup (CommandLineRunner) to seed sample rooms,
// since H2 is in-memory and starts empty every time.
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoomPricingTierRepository tierRepository;
    private final RoomRepository roomRepository;

    public DataSeeder(RoomPricingTierRepository tierRepository, RoomRepository roomRepository) {
        this.tierRepository = tierRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(String... args) {

        // Avoids creating duplicate rooms every time the app restarts.
        if (roomRepository.count() > 0) {
            return;
        }

        // Pricing Tiers aligned with hotel offerings
        RoomPricingTier tier1 = new RoomPricingTier();
        tier1.setTierName("The Front Loft (Room 01)");
        tier1.setBaseRate(new BigDecimal("19500.00"));
        tierRepository.save(tier1);

        RoomPricingTier tier2 = new RoomPricingTier();
        tier2.setTierName("The Cozy Loft (Room 02)");
        tier2.setBaseRate(new BigDecimal("16500.00"));
        tierRepository.save(tier2);

        RoomPricingTier tier3 = new RoomPricingTier();
        tier3.setTierName("Entire Lodge Buyout");
        tier3.setBaseRate(new BigDecimal("35000.00"));
        tierRepository.save(tier3);

        Room room1 = new Room();
        room1.setRoomNumber("1");
        room1.setRoomType("The Front Loft (Suite 01)");
        room1.setDescription("Our larger room situated at the front of the stilted structure with spacious loft beds.");
        room1.setTier(tier1);
        roomRepository.save(room1);

        Room room2 = new Room();
        room2.setRoomNumber("2");
        room2.setRoomType("The Cozy Loft (Suite 02)");
        room2.setDescription("Intimate room with identical earthy clay wall finishes overlooking village paddy fields.");
        room2.setTier(tier2);
        roomRepository.save(room2);

        // Room 3: Entire Lodge Buyout (Both Suites)
        Room room3 = new Room();
        room3.setRoomNumber("3");
        room3.setRoomType("Entire Lodge Buyout (Both Suites)");
        room3.setDescription("Guaranteed exclusive stay of the entire sanctuary covering both suites up to 10 guests.");
        room3.setTier(tier3);
        roomRepository.save(room3);
    }
}