package com.loft.hotel.controller;

import com.loft.hotel.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InquiryPageController {

    private final InquiryService inquiryService;

    @Autowired
    public InquiryPageController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/inquiry")
    public String showInquiryForm() {
        return "inquiry";
    }

    @PostMapping("/inquiry")
    public String submitInquiry(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phoneNo,
                                @RequestParam String subject,
                                @RequestParam String message) {
        inquiryService.submitInquiry(fullName, email, phoneNo, subject, message);
        return "inquiry-success";
    }
}
