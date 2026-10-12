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

        // Both rooms are identical, so they share one pricing tier.
        RoomPricingTier standard = new RoomPricingTier();
        standard.setTierName("Standard");
        standard.setBaseRate(new BigDecimal("10000"));
        tierRepository.save(standard);

        Room room1 = new Room();
        room1.setRoomNumber("1");
        room1.setRoomType("Standard");
        room1.setDescription("Room 1");
        room1.setTier(standard);
        roomRepository.save(room1);

        // Separate row with its own ID, so booking Room 1 never blocks Room 2.
        Room room2 = new Room();
        room2.setRoomNumber("2");
        room2.setRoomType("Standard");
        room2.setDescription("Room 2");
        room2.setTier(standard);
        roomRepository.save(room2);
    }
}