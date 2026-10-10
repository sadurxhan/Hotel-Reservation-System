package com.loft.hotel.repository;

import com.loft.hotel.model.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {

    // "find inquiries WHERE inquiry_status = ?  ORDER BY inquiry_date DESC"
    List<Inquiry> findByInquiryStatusOrderByInquiryDateDesc(String inquiryStatus);

    // all inquiries, newest first (for the admin inbox)
    List<Inquiry> findAllByOrderByInquiryDateDesc();
}