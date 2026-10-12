package com.loft.hotel.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestLookupService {

    // JdbcTemplate runs raw SQL directly - this is how we avoid needing a Guest.java entity
    private final JdbcTemplate jdbcTemplate;

    public GuestLookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the guest_id for this email.
     * If no guest has this email yet, a new guest row is created first.
     */
    public Integer findOrCreateGuest(String fullName, String email, String phoneNo) {

        String cleanEmail = email.trim();

        // Split "Vinuthi Gunasekara" into first name + last name.
        // The 2 means "split into at most 2 pieces", so "Mary Ann Smith" -> "Mary" + "Ann Smith"
        String[] nameParts = fullName.trim().split("\\s+", 2);
        String fname = nameParts[0];
        String lname = nameParts.length > 1 ? nameParts[1] : "";   // lname is NOT NULL in the DB, so "" not null

        // 1. Does a guest with this email already exist?
        List<Integer> existing = jdbcTemplate.queryForList(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, cleanEmail);

        if (!existing.isEmpty()) {
            return existing.get(0);      // yes -> reuse their id, no duplicate
        }

        // 2. No match -> create a new guest row
        try {
            jdbcTemplate.update(
                    "INSERT INTO guest (fname, lname, email, phone_no) VALUES (?, ?, ?, ?)",
                    fname, lname, cleanEmail, phoneNo.trim());
        } catch (DuplicateKeyException e) {
            // The email column is UNIQUE. If another request inserted this same email
            // a split second ago, MySQL rejects ours - that's fine, we just read theirs below.
        }

        // 3. Read the id back (works whether we inserted it or someone else did)
        return jdbcTemplate.queryForObject(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, cleanEmail);
    }
}
