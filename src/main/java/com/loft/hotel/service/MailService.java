package com.loft.hotel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
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
            log.info("[MailService - Simulation Log] To: {} | Subject: {}", to, subject);
            return true;
        }

        // Send email in a background thread so slow/unreachable SMTP servers never block the HTTP response
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.warn("Failed to send email to {}: {}", to, e.getMessage());
            }
        });

        return true;
    }
}