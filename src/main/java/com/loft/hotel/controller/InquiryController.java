package com.loft.hotel.controller;

import com.loft.hotel.model.Inquiry;
import com.loft.hotel.service.InquiryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    // GET /api/inquiry/all  -> every inquiry, newest first
    @GetMapping("/all")
    public List<Inquiry> getAllInquiries() {
        return inquiryService.getAllInquiries();
    }

    // GET /api/inquiry/status/PENDING  -> only inquiries still waiting for a reply
    @GetMapping("/status/{status}")
    public List<Inquiry> getByStatus(@PathVariable String status) {
        return inquiryService.getInquiriesByStatus(status);
    }

    // PUT /api/inquiry/update-status/3?status=RESOLVED
    @PutMapping("/update-status/{inquiryId}")
    public Inquiry updateStatus(@PathVariable Integer inquiryId,
                                @RequestParam String status) {
        return inquiryService.updateStatus(inquiryId, status);
    }
}
