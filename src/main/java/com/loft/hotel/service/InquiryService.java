package com.loft.hotel.service;

import com.loft.hotel.model.Inquiry;
import com.loft.hotel.repository.InquiryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);

    private final InquiryRepository inquiryRepository;
    private final GuestLookupService guestLookupService;
    private final JavaMailSender mailSender;

    // @Value reads a setting from application.properties (the text after ":" is the default)
    @Value("${app.owner.email:}")
    private String ownerEmail;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public InquiryService(InquiryRepository inquiryRepository,
                          GuestLookupService guestLookupService,
                          JavaMailSender mailSender) {
        this.inquiryRepository = inquiryRepository;
        this.guestLookupService = guestLookupService;
        this.mailSender = mailSender;
    }

    // ---------- Guest side ----------

    public Inquiry submitInquiry(String fullName, String email, String phoneNo,
                                 String subject, String message) {

        // Server-side validation (the browser's "required" check can be bypassed)
        if (isBlank(fullName) || isBlank(email) || isBlank(phoneNo)
                || isBlank(subject) || isBlank(message)) {
            throw new IllegalArgumentException("All fields are required");
        }

        // 1. find or create the guest
        Integer guestId = guestLookupService.findOrCreateGuest(fullName, email, phoneNo);

        // 2. build and save the inquiry
        Inquiry inquiry = new Inquiry();
        inquiry.setGuestId(guestId);
        inquiry.setInquirySubject(subject);
        inquiry.setMessage(message);
        inquiry.setInquiryDate(LocalDateTime.now());
        inquiry.setInquiryStatus("PENDING");
        Inquiry saved = inquiryRepository.save(inquiry);

        // 3. tell the owner (never allowed to break the submission itself)
        notifyOwner(saved, fullName, email, phoneNo);

        return saved;
    }

    // ---------- Admin side ----------

    public List<Inquiry> getAllInquiries() {
        return inquiryRepository.findAllByOrderByInquiryDateDesc();
    }

    public List<Inquiry> getInquiriesByStatus(String status) {
        return inquiryRepository.findByInquiryStatusOrderByInquiryDateDesc(status.toUpperCase());
    }

    public Inquiry updateStatus(Integer inquiryId, String newStatus) {
        String status = newStatus.toUpperCase();
        if (!status.equals("PENDING") && !status.equals("RESOLVED")) {
            throw new IllegalArgumentException("Status must be PENDING or RESOLVED");
        }
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));
        inquiry.setInquiryStatus(status);
        return inquiryRepository.save(inquiry);
    }

    // ---------- helpers ----------

    private void notifyOwner(Inquiry inquiry, String fullName, String email, String phoneNo) {
        if (isBlank(ownerEmail)) {
            log.info("Owner email not configured - skipping notification");
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            if (!isBlank(fromEmail)) {
                mail.setFrom(fromEmail);
            }
            mail.setTo(ownerEmail);
            mail.setSubject("New inquiry: " + inquiry.getInquirySubject());
            mail.setText("A new inquiry was submitted on The Loft by the Lake website.\n\n"
                    + "Name:    " + fullName + "\n"
                    + "Email:   " + email + "\n"
                    + "Phone:   " + phoneNo + "\n"
                    + "Subject: " + inquiry.getInquirySubject() + "\n\n"
                    + "Message:\n" + inquiry.getMessage());
            mailSender.send(mail);
        } catch (Exception e) {
            // If email fails (wrong password, no internet...) the inquiry is ALREADY saved.
            // We just log it instead of showing the guest an error.
            log.warn("Could not send owner notification: {}", e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}