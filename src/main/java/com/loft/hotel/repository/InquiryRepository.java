package com.loft.hotel.repository;

import com.loft.hotel.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {
}
