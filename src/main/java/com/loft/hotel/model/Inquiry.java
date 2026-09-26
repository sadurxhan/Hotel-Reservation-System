package com.loft.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Integer inquiryId;

    @Column(name = "guest_id", nullable = false)
    private Integer guestId;

    @Column(name = "inquiry_subject", nullable = false, length = 100)
    private String inquirySubject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "inquiry_date")
    private LocalDateTime inquiryDate;

    @Column(name = "inquiry_status", length = 20)
    private String inquiryStatus = "PENDING";

    public Integer getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(Integer inquiryId) {
        this.inquiryId = inquiryId;
    }

    public Integer getGuestId() {
        return guestId;
    }

    public void setGuestId(Integer guestId) {
        this.guestId = guestId;
    }

    public String getInquirySubject() {
        return inquirySubject;
    }

    public void setInquirySubject(String inquirySubject) {
        this.inquirySubject = inquirySubject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getInquiryDate() {
        return inquiryDate;
    }

    public void setInquiryDate(LocalDateTime inquiryDate) {
        this.inquiryDate = inquiryDate;
    }

    public String getInquiryStatus() {
        return inquiryStatus;
    }

    public void setInquiryStatus(String inquiryStatus) {
        this.inquiryStatus = inquiryStatus;
    }


    // getters and setters
}