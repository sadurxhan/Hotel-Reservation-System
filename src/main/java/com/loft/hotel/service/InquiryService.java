package com.loft.hotel.service;
// Services live in their own "service" package — this is where the
// actual decision-making/logic happens, separate from data storage.

import com.loft.hotel.model.Inquiry;
import com.loft.hotel.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
// This annotation tells Spring "this class contains business logic —
// manage it for me, and let other classes (like controllers) use it."
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    // We store a reference to the repository here so this class can
    // actually reach the database through it.

    @Autowired
    // This tells Spring: "automatically hand me a working InquiryRepository
    // when this class is created — I don't want to build it myself."
    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    public Inquiry submitInquiry(Integer guestId, String subject, String message) {
        // This method will be called when a guest submits the contact form.
        // TODO: once Guest.java exists, replace guestId param with
        // name/email and look up-or-create the Guest here instead.

        Inquiry inquiry = new Inquiry();
        // Create a blank Inquiry object in memory (nothing saved yet).

        inquiry.setGuestId(guestId);
        inquiry.setInquirySubject(subject);
        inquiry.setMessage(message);
        inquiry.setInquiryDate(LocalDateTime.now());
        // .now() grabs the current date/time automatically.
        inquiry.setInquiryStatus("PENDING");
        // Every new inquiry starts as PENDING until the owner reads it.

        return inquiryRepository.save(inquiry);
        // .save() is one of those free methods JpaRepository gave us —
        // this is the line that actually writes the row into MySQL.
    }

    public List<Inquiry> getAllInquiries() {
        // This will be used by the owner/admin side to see every inquiry.
        return inquiryRepository.findAll();
        // findAll() is also free from JpaRepository — grabs every row.
    }

    public Inquiry updateStatus(Integer inquiryId, String newStatus) {
        // Lets the admin mark an inquiry as RESOLVED, etc.
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));
        // findById returns something called an "Optional" — a safe box that
        // might be empty. .orElseThrow() says "if it's empty, crash with
        // this error message" instead of silently giving us nothing.

        inquiry.setInquiryStatus(newStatus);
        return inquiryRepository.save(inquiry);
        // Calling .save() on an object that already has an ID updates
        // the existing row instead of creating a new one.
    }
}
