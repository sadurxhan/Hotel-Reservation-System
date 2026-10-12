package com.loft.hotel.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Simple admin check shared by all admin endpoints (X-Admin-Key header)
@Component
public class AdminGuard {

    @Value("${app.admin-key}")
    private String adminKey;

    // Fail at startup (not on every request) if the key is missing or blank.
    @PostConstruct
    void validateConfiguration() {
        if (adminKey == null || adminKey.isBlank()) {
            throw new IllegalStateException("app.admin-key must be set.");
        }
    }

    public void check(String key) {
        // constant-time comparison so response timing doesn't leak the key
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Admin access required.");
        }
    }
}