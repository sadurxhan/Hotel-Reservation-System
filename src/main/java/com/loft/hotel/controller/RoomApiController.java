package com.loft.hotel.controller;

import com.loft.hotel.entity.Room;
import com.loft.hotel.repository.RoomRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomApiController {

    private final RoomRepository roomRepository;

    public RoomApiController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}