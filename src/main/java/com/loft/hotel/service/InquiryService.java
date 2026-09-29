package com.loft.hotel.service;

import com.loft.hotel.entity.Inquiry;
import com.loft.hotel.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final JdbcTemplate jdbcTemplate;
    // JdbcTemplate lets us run raw SQL directly, without needing a
    // Guest.java entity class — just what we need for this workaround.

    @Autowired
    public InquiryService(InquiryRepository inquiryRepository, JdbcTemplate jdbcTemplate) {
        this.inquiryRepository = inquiryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Integer findOrCreateGuest(String fname, String lname, String email, String phoneNo) {
        // Step 1: check if a guest with this email already exists.
        List<Integer> existing = jdbcTemplate.queryForList(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, email
        );

        if (!existing.isEmpty()) {
            return existing.get(0);
            // Guest already exists — reuse their ID, don't create a duplicate.
        }

        // Step 2: no match found — insert a new guest row.
        java.sql.PreparedStatement[] ps = new java.sql.PreparedStatement[1];
        jdbcTemplate.update(connection -> {
            ps[0] = connection.prepareStatement(
                    "INSERT INTO guest (fname, lname, email, phone_no) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps[0].setString(1, fname);
            ps[0].setString(2, lname);
            ps[0].setString(3, email);
            ps[0].setString(4, phoneNo);
            return ps[0];
        });

        // Grab the auto-generated ID of the guest we just inserted.
        return jdbcTemplate.queryForObject(
                "SELECT guest_id FROM guest WHERE email = ?", Integer.class, email
        );
    }

    public Inquiry submitInquiry(String fullName, String email, String phoneNo,
                                 String subject, String message) {
        // Split "Full Name" into first/last for the guest table's two columns.
        String[] nameParts = fullName.trim().split(" ", 2);
        String fname = nameParts[0];
        String lname = nameParts.length > 1 ? nameParts[1] : "";

        Integer guestId = findOrCreateGuest(fname, lname, email, phoneNo);

        Inquiry inquiry = new Inquiry();
        inquiry.setGuestId(guestId);
        inquiry.setInquirySubject(subject);
        inquiry.setMessage(message);
        inquiry.setInquiryDate(LocalDateTime.now());
        inquiry.setInquiryStatus("PENDING");

        return inquiryRepository.save(inquiry);
    }

    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAll();
    }

    public Inquiry updateStatus(Integer inquiryId, String newStatus) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));
        inquiry.setInquiryStatus(newStatus);
        return inquiryRepository.save(inquiry);
    }
}
