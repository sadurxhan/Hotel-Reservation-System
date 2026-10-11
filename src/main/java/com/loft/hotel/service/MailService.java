package com.loft.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Autowired
    public MailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Required by InvoiceService, RefundService, and CancellationService
    public boolean send(String to, String subject, String body) {
        return sendEmail(to, subject, body);
    }

    public boolean sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            System.out.println("[MailService - Simulation Log] To: " + to + " | Subject: " + subject);
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }
}