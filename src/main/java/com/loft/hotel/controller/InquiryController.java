package com.loft.hotel.controller;

import com.loft.hotel.entity.Inquiry;
import com.loft.hotel.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    @Autowired
    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/all")
    public List<Inquiry> getAllInquiries() {
        return inquiryService.getAllInquiries();
    }

    @PutMapping("/update-status/{inquiryId}")
    public Inquiry updateStatus(@PathVariable Integer inquiryId,
                                @RequestParam String status) {
        return inquiryService.updateStatus(inquiryId, status);
    }
}
