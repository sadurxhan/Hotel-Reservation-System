package com.loft.hotel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// assignableTypes = ONLY these controllers are covered (all Member 4's)
@RestControllerAdvice(assignableTypes = {
        InquiryController.class, InquiryPageController.class,
        ReviewController.class, ReviewPageController.class,
        MenuShowcaseController.class, ActivityController.class
})
public class GlobalExceptionHandler {

    // our own validation errors -> 400 Bad Request with the message
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // photo bigger than the 5MB limit
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleTooBig(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File is too large (max 5MB)");
    }
}