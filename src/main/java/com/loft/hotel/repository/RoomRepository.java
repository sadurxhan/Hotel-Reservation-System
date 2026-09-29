package com.loft.hotel.repository;

import com.loft.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {

    Optional<Room> findByRoomNumber(String roomNumber);
}
