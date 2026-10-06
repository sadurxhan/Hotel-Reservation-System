package com.loft.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application Entry Point.
 *
 * @EnableScheduling activates Spring's background task execution engine,
 * enabling @Scheduled background job: periodic iCal calendar synchronizations.
 */
@SpringBootApplication
@EnableScheduling
public class HotelApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelApplication.class, args);
    }
}

