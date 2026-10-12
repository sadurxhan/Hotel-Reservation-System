show databases;
select database();

-- Creating and selecting the database
CREATE DATABASE hotel_reservation_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
USE hotel_reservation_db;

-- =======================
-- 1. CREATING DB TABLES
-- =======================

-- Base lookup and tables

-- IT25102812
CREATE TABLE guest (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_no VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- IT25102812
CREATE TABLE room (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type VARCHAR(100) NOT NULL,
    room_description TEXT,
    capacity INT NOT NULL,
    room_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    tier_id INT NULL
);

-- IT25102812
CREATE TABLE room_pricing_tier (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    guest_count INT NOT NULL,
    tier_name VARCHAR(50) NOT NULL,
    base_rate DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_tier_room FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE CASCADE,
    CONSTRAINT uq_room_guest_tier UNIQUE (room_id, guest_count)
);

-- Main tables used for bookings

-- IT25102812
CREATE TABLE reservation (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    number_of_guests INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    reservation_status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE,
    CONSTRAINT chk_dates CHECK (check_out_date > check_in_date)
);

-- IT25102812
CREATE TABLE reservation_room_selection (
    selection_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    room_id INT NOT NULL,
    no_of_guests INT NOT NULL,
    calc_room_price_per_night DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_selection_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_selection_room FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE RESTRICT
);

-- Payment and billing tables

-- IT25102967
CREATE TABLE payment (
    payment_id VARCHAR(40) PRIMARY KEY,
    reservation_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method ENUM('Credit_Card', 'Debit_Card', 'Online_Banking', 'QR_Payment') NOT NULL,
    payment_status ENUM('PENDING', 'PAID', 'FAILED') NOT NULL,
    transaction_id VARCHAR(40) UNIQUE,
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- IT25102967
CREATE TABLE refund (
    refund_id VARCHAR(40) PRIMARY KEY,
    payment_id VARCHAR(40) NOT NULL,
    refund_status ENUM('PENDING', 'APPROVED', 'REFUNDED', 'REJECTED') NOT NULL,
    refund_reason VARCHAR(300) NOT NULL,
    refund_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refund_amount DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- IT25102967
CREATE TABLE invoice (
    invoice_id VARCHAR(40) PRIMARY KEY,
    payment_id VARCHAR(40) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    invoice_status ENUM('ISSUED', 'SENT') NOT NULL,
    issued_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- IT25102967
CREATE TABLE cancellation (
    cancellation_id VARCHAR(40) PRIMARY KEY,
    reservation_id INT NOT NULL UNIQUE,
    requested_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requested_by ENUM('Guest', 'Admin') NOT NULL,
    cancellation_reason VARCHAR(300) NOT NULL,
    cancellation_status ENUM('REQUESTED', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'REQUESTED',
    CONSTRAINT fk_cancellation_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Calendar Block table (Availability & iCal)

-- IT25102998
CREATE TABLE IF NOT EXISTS calendar_block (
    block_id INT AUTO_INCREMENT PRIMARY KEY,
    blocked_date DATE NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL_ADMIN',
    external_uid VARCHAR(300) NULL,
    room_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calendar_block_room FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE CASCADE
);

-- Table for dynamic rate overrides and packages

-- IT25102998
CREATE TABLE rate_override (
    rate_id INT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(100) NULL,
    package_description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rate_multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
    min_nights INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    room_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rate_override_room FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE SET NULL
);

-- Guest experience and inquiries

-- IT25102954
CREATE TABLE inquiry (
    inquiry_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    inquiry_subject VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    inquiry_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    inquiry_status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT fk_inquiry_guest FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE
);

-- IT25102954
CREATE TABLE review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    rating INT NOT NULL,
    review_status VARCHAR(20) DEFAULT 'PENDING',
    review_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    review_comment TEXT,
    CONSTRAINT fk_review_guest FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

-- IT25102954
CREATE TABLE menu_showcase (
    menu_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    meal_type VARCHAR(50) NOT NULL,
    meal_description TEXT,
    file_url VARCHAR(300),
    is_available BOOLEAN DEFAULT TRUE
);

-- IT25102954
CREATE TABLE activity (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    activity_description TEXT,
    pricing_note VARCHAR(300),
    image_url VARCHAR(300),
    is_active BOOLEAN DEFAULT TRUE
);

-- Performance indexes for search and availability
CREATE INDEX idx_calendar_block_date ON calendar_block(blocked_date);
CREATE INDEX idx_rate_override_dates ON rate_override(start_date, end_date);

-- =================
-- DATA POPULATION
-- =================

-- Base lookup and tables

-- IT25102812
INSERT INTO guest (fname, lname, email, phone_no) VALUES
('Sashini', 'Jayasundara', 'sashinij@gmail.com', '+94712345678'),
('Nethmi', 'Perera', 'nethmi.p@gmail.com', '+94771234567'),
('Vinuthi', 'Silva', 'vinuthi.s@yahoo.com', '+94709876543');

-- IT25102812
INSERT INTO room (room_id, room_number, room_type, room_description, capacity, room_status, is_active, tier_id) VALUES
(1, '101', 'Deluxe Ocean View', 'Spacious king-bed room with sea view', 2, 'AVAILABLE', TRUE, 1),
(2, '102', 'Standard Double', 'Comfortable double bed room', 2, 'AVAILABLE', TRUE, 2),
(3, '201', 'Family Suite', 'Two-bedroom suite with living area', 4, 'AVAILABLE', TRUE, NULL),
(4, '202', 'Executive Suite', 'Luxury suite with private balcony', 3, 'MAINTENANCE', TRUE, NULL);

-- IT25102812
INSERT INTO room_pricing_tier (tier_id, room_id, guest_count, tier_name, base_rate) VALUES
(1, 1, 1, 'Single Occupancy', 10000.00),
(2, 1, 2, 'Double Occupancy', 15000.00),
(3, 2, 2, 'Standard Rate', 9000.00),
(4, 3, 4, 'Family Package', 25000.00);

-- Main tables used for bookings

-- IT25102812
INSERT INTO reservation (reservation_id, guest_id, check_in_date, check_out_date, booking_date, number_of_guests, total_amount, reservation_status) VALUES
(101, 1, '2026-10-01', '2026-10-05', '2026-09-20 10:30:00', 2, 60000.00, 'CONFIRMED'),
(102, 2, '2026-10-10', '2026-10-12', '2026-09-21 14:15:00', 4, 50000.00, 'PENDING'),
(103, 3, '2026-10-15', '2026-10-18', '2026-09-22 09:00:00', 1, 27000.00, 'CONFIRMED'),
(104, 1, '2026-11-01', '2026-11-03', '2026-09-23 16:45:00', 2, 30000.00, 'CANCELLED');

-- IT25102812
INSERT INTO reservation_room_selection (selection_id, reservation_id, room_id, no_of_guests, calc_room_price_per_night) VALUES
(1, 101, 1, 2, 15000.00),
(2, 102, 3, 4, 25000.00),
(3, 103, 2, 1, 9000.00),
(4, 104, 1, 2, 15000.00);


-- Payment and billing tables

-- IT25102967
INSERT INTO payment VALUES ('PAY001', 101, 60000.00, '2026-09-20 10:35:00', 'Credit_Card', 'PAID', 'TXN001');
INSERT INTO payment VALUES ('PAY002', 102, 25000.00, '2026-09-21 14:20:00', 'QR_Payment', 'PAID', 'TXN002');
INSERT INTO payment VALUES ('PAY003', 103, 27000.00, '2026-09-22 09:05:00', 'Online_Banking', 'FAILED', 'TXN003');

-- IT25102967
INSERT INTO refund VALUES ('REF001', 'PAY001', 'REFUNDED', '100% refund applied according to cancellation policy', '2026-09-25 10:00:00', 60000.00);
INSERT INTO refund VALUES ('REF002', 'PAY002', 'APPROVED', 'Remaining stay cancelled by owner', '2026-09-26 11:00:00', 25000.00);

-- IT25102967
INSERT INTO invoice VALUES ('INV001', 'PAY001', 60000.00, 'SENT', '2026-09-20 10:36:00');
INSERT INTO invoice VALUES ('INV002', 'PAY002', 25000.00, 'ISSUED', '2026-09-21 14:21:00');

-- IT25102967
INSERT INTO cancellation VALUES('CAN001', 101, '2026-09-25 10:00:00', 'Guest', 'Guest changed travel plans', 'APPROVED');
INSERT INTO cancellation VALUES('CAN002', 102, '2026-09-26 11:00:00', 'Admin', 'Booking cancelled by admin', 'APPROVED');

-- Calendar Block table (Availability & iCal)

-- IT25102998
-- Pre-block dates for reservation 101 (Room 1: Oct 1 - Oct 4) and manual maintenance block
INSERT INTO calendar_block (blocked_date, source, external_uid, room_id) VALUES
('2026-10-01', 'INTERNAL_BOOKING', NULL, 1),
('2026-10-02', 'INTERNAL_BOOKING', NULL, 1),
('2026-10-03', 'INTERNAL_BOOKING', NULL, 1),
('2026-10-04', 'INTERNAL_BOOKING', NULL, 1),
('2026-10-20', 'MANUAL_ADMIN', NULL, 2),
('2026-10-21', 'MANUAL_ADMIN', NULL, 2);

-- IT25102998
-- Sample seasonal and weekend packages
INSERT INTO rate_override (package_name, package_description, start_date, end_date, rate_multiplier, min_nights, is_active, room_id) VALUES
('Weekend Getaway', '15% surcharge for peak weekend bookings', '2026-10-01', '2026-10-31', 1.15, 2, TRUE, NULL),
('Long Stay Discount', '10% discount for stays over 5 nights', '2026-11-01', '2026-11-30', 0.90, 5, TRUE, 1);


-- Guest experience and inquiries

-- IT25102954
INSERT INTO inquiry (guest_id, inquiry_subject, message, inquiry_status) VALUES
(1, 'Pets & Policies', 'Can we bring a cat to the villa?', 'PENDING'),
(2, 'Booking Question', 'Do you offer airport pickup?', 'PENDING'),
(3, 'General Inquiry', 'Is the lake safe for swimming in October?', 'RESOLVED');

-- IT25102954
INSERT INTO review (guest_id, rating, review_status, review_comment) VALUES
(1, 5, 'APPROVED', 'Absolutely stunning views, will come back!'),
(2, 4, 'PENDING', 'Great stay, breakfast could be better.'),
(3, 5, 'APPROVED', 'Perfect for a quiet weekend getaway.');

-- IT25102954
INSERT INTO menu_showcase (title, meal_type, meal_description, is_available) VALUES
('Sri Lankan Rice & Curry', 'LUNCH', 'Steamed rice with four vegetable and meat curries', TRUE),
('BBQ Dinner by the Lake', 'DINNER', 'Grilled seafood and chicken platter served lakeside', TRUE),
('Ceylon Breakfast Set', 'BREAKFAST', 'String hoppers, dhal curry, and coconut sambol', TRUE);

-- IT25102954
INSERT INTO activity (title, activity_description, pricing_note, is_active) VALUES
('Lake Kayaking', 'Paddle around the lake at sunset', 'Complimentary for guests', TRUE),
('Lakeside Cycling', 'Guided cycling tour around the paddy fields', 'LKR 1,500 per person', TRUE),
('Sunset Bird Watching', 'Spot local bird species along the shoreline', 'Complimentary for guests', TRUE);

-- ==================================
-- VERIFICATION & ANALYTICS QUERIES
-- ==================================

-- Base table verification
SELECT * FROM guest;
SELECT * FROM room;
SELECT * FROM room_pricing_tier;
SELECT * FROM reservation;
SELECT * FROM reservation_room_selection;
SELECT * FROM calendar_block;
SELECT * FROM rate_override;
SELECT * FROM inquiry;
SELECT * FROM review;
SELECT * FROM menu_showcase;
SELECT * FROM activity;

-- ============================================
-- queries IT25102812 (Reservation life cycle)
-- ============================================
-- Calculate Total Revenue Generated by Reservation Status
SELECT reservation_status,
    COUNT(reservation_id) AS total_reservations,
    SUM(total_amount) AS total_revenue
FROM reservation
GROUP BY reservation_status;

-- Find Guests with Multiple Active or Past Reservations
SELECT guest.guest_id,
    CONCAT(guest.fname, ' ', guest.lname) AS guest_full_name,
    guest.email,
    COUNT(reservation.reservation_id) AS total_bookings_placed
FROM guest
JOIN reservation ON guest.guest_id = reservation.guest_id
GROUP BY guest.guest_id, guest.fname, guest.lname, guest.email
HAVING COUNT(reservation.reservation_id) > 1;

-- Extend or Modify Check-Out Date and Recalculate Total
UPDATE reservation
SET check_out_date = '2026-10-07',
    total_amount = 900.00
WHERE reservation_id = 101
  AND reservation_status = 'CONFIRMED';
  
-- Cancel Pending Reservation 
UPDATE reservation 
SET reservation_status = 'CANCELLED' 
WHERE reservation_id = 102 
  AND reservation_status = 'PENDING';

-- ==============================================
-- queries IT25102967 (Billing and cancellation)
-- ==============================================
-- Display all successful payments
SELECT * FROM payment WHERE payment_status = 'PAID';

-- Find payment methods with total successful payments above 50,000
SELECT payment_method, COUNT(*) AS payment_count, SUM(amount) AS total_amount FROM payment
WHERE payment_status = 'PAID' GROUP BY payment_method HAVING SUM(amount) >= 50000;

-- Find the total amount paid by each guest
SELECT res.guest_id, SUM(p.amount) AS total_paid FROM payment p 
JOIN reservation res ON p.reservation_id = res.reservation_id
WHERE p.payment_status = 'PAID' GROUP BY res.guest_id;

-- Find the largest payment recorded
SELECT * FROM payment ORDER BY amount DESC LIMIT 1;

-- Display refunds with the original payment amount
SELECT r.refund_id, r.payment_id, r.refund_amount, p.amount AS original_amount, ROUND((r.refund_amount / p.amount) * 100, 1) AS percent_refunded
FROM refund r JOIN payment p ON r.payment_id = p.payment_id WHERE r.refund_status = 'REFUNDED';

-- Display refunds that are still being processed
SELECT * FROM refund WHERE refund_status IN ('PENDING', 'APPROVED');

-- Display invoices that were issued but not yet sent in this month
SELECT * FROM invoice WHERE invoice_status = 'ISSUED' AND MONTH(issued_datetime) = MONTH(CURRENT_DATE()) AND YEAR(issued_datetime) = YEAR(CURRENT_DATE());

-- Display invoice details with payment details
SELECT i.invoice_id, i.payment_id, i.total_amount, i.invoice_status, i.issued_datetime, p.payment_method, p.payment_status
FROM invoice i JOIN payment p ON i.payment_id = p.payment_id;

-- Calculate the total collected amount, total refunded amount, and the remaining net revenue
SELECT (SELECT SUM(amount) FROM payment WHERE payment_status = 'PAID') AS total_collected,
(SELECT SUM(refund_amount) FROM refund WHERE refund_status = 'REFUNDED') AS total_refunded,
(SELECT SUM(amount) FROM payment WHERE payment_status = 'PAID') - 
COALESCE((SELECT SUM(refund_amount) FROM refund WHERE refund_status = 'REFUNDED'), 0) AS net_revenue;


-- ====================================================
-- queries IT25102998 (Pricing and calendar management)
-- ====================================================
-- View all calendar block dates with associated room numbers and room types
SELECT cb.block_id, cb.blocked_date, cb.source, r.room_number, r.room_type, r.room_status
FROM calendar_block cb
LEFT JOIN room r ON cb.room_id = r.room_id
ORDER BY cb.blocked_date ASC;

-- Count unavailable/blocked dates grouped by blocking source
SELECT source, COUNT(*) AS total_blocked_days
FROM calendar_block
GROUP BY source;

-- find rooms currently blocked for a targeted stay date range
SELECT r.room_id, r.room_number, r.room_type, cb.blocked_date, cb.source
FROM room r
JOIN calendar_block cb ON r.room_id = cb.room_id
WHERE cb.blocked_date BETWEEN '2026-10-01' AND '2026-10-05';

-- Display all active seasonal rate packages and their applicable room details
SELECT ro.package_name, ro.start_date, ro.end_date, ro.rate_multiplier, ro.min_nights,
       COALESCE(r.room_number, 'ALL ROOMS') AS applicable_room
FROM rate_override ro
LEFT JOIN room r ON ro.room_id = r.room_id
WHERE ro.is_active = TRUE
ORDER BY ro.start_date ASC;

-- Calculate effective dynamic room price by applying active rate multipliers to base rates
SELECT r.room_number, r.room_type, rpt.tier_name, rpt.base_rate,
       ro.package_name, ro.rate_multiplier,
       ROUND(rpt.base_rate * ro.rate_multiplier, 2) AS calculated_dynamic_price
FROM room r
JOIN room_pricing_tier rpt ON r.room_id = rpt.room_id
JOIN rate_override ro ON (ro.room_id = r.room_id OR ro.room_id IS NULL)
WHERE ro.is_active = TRUE;

-- Clean up historical calendar blocks
DELETE FROM calendar_block 
WHERE blocked_date < '2026-09-01';


-- =============================================
-- queries IT25102954 (Inquiries and activities)
-- =============================================
-- Display all pending inquiries the owner still needs to respond to
SELECT i.inquiry_id, g.fname, g.lname, i.inquiry_subject, i.message, i.inquiry_date
FROM inquiry i
JOIN guest g ON i.guest_id = g.guest_id
WHERE i.inquiry_status = 'PENDING';

-- Count how many inquiries each guest has submitted
SELECT g.guest_id, CONCAT(g.fname, ' ', g.lname) AS guest_name, COUNT(i.inquiry_id) AS total_inquiries
FROM guest g
JOIN inquiry i ON g.guest_id = i.guest_id
GROUP BY g.guest_id, g.fname, g.lname;

-- Calculate the average rating across all approved reviews
SELECT ROUND(AVG(rating), 1) AS average_rating, COUNT(*) AS total_approved_reviews
FROM review
WHERE review_status = 'APPROVED';

-- Display approved reviews with the guest's name attached
SELECT r.review_id, CONCAT(g.fname, ' ', g.lname) AS guest_name, r.rating, r.review_comment, r.review_date
FROM review r
JOIN guest g ON r.guest_id = g.guest_id
WHERE r.review_status = 'APPROVED';

-- Display available menu items grouped by meal type
SELECT meal_type, COUNT(*) AS item_count
FROM menu_showcase
WHERE is_available = TRUE
GROUP BY meal_type;

-- Display all currently active activities
SELECT title, activity_description, pricing_note
FROM activity
WHERE is_active = TRUE;

