package com.loft.hotel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;

    @Value("${spring.mail.username}")
    private String from;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    // Never breaks the main flow: returns false if the email fails
    public boolean send(String to, String subject, String body) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            sender.send(m);
            return true;
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
            return false;
        }
    }
}