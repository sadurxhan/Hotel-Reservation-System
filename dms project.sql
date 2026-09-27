-- Creating and selecting the database
CREATE DATABASE IF NOT EXISTS hotel_reservation_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
USE hotel_reservation_db;

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
    room_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
);

-- IT25102812
CREATE TABLE room_pricing_tier (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    guest_count INT NOT NULL,
    tier_name VARCHAR(50) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
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

-- IT25102###
CREATE TABLE payment (
    payment_id VARCHAR(40) PRIMARY KEY,
    reservation_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method ENUM('Credit_Card', 'Debit_Card', 'Online_Banking', 'QR_Payment') NOT NULL,
	payment_status ENUM('PENDING', 'PAID', 'FAILED') NOT NULL,
    transaction_id VARCHAR(15) UNIQUE,
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- IT25102###
CREATE TABLE refund (
    refund_id VARCHAR(40) PRIMARY KEY,
    payment_id VARCHAR(40) NOT NULL,
    refund_type ENUM('Guest_Initiated', 'Admin_Initiated') NOT NULL,
    refund_status ENUM('PENDING', 'APPROVED', 'REFUNDED', 'REJECTED') NOT NULL,
    refund_reason VARCHAR(300) NOT NULL,
    refund_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refund_amount DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- IT25102###
CREATE TABLE invoice (
    invoice_id VARCHAR(40) PRIMARY KEY,
    payment_id VARCHAR(40) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    invoice_status ENUM('ISSUED', 'SENT') NOT NULL,
    issued_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE RESTRICT ON UPDATE CASCADE
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
CREATE TABLE IF NOT EXISTS rate_override (
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
    
-- Performance indexes for search and availability
CREATE INDEX idx_calendar_block_date ON calendar_block(blocked_date);
CREATE INDEX idx_rate_override_dates ON rate_override(start_date, end_date);

-- Guest experience and inquiries

-- IT2510
CREATE TABLE inquiry (
    inquiry_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    inquiry_subject VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    inquiry_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    inquiry_status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT fk_inquiry_guest FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE
);

-- IT25102###
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

-- IT25102###
CREATE TABLE menu_showcase (
    menu_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    meal_type VARCHAR(50) NOT NULL,
    meal_description TEXT,
    file_url VARCHAR(300),
    is_available BOOLEAN DEFAULT TRUE
);

-- IT25102###
CREATE TABLE activity (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    activity_description TEXT,
    pricing_note VARCHAR(300),
    image_url VARCHAR(300),
    is_active BOOLEAN DEFAULT TRUE
);

-- Indexes for lightning-fast date range queries during booking search
CREATE INDEX idx_calendar_block_date ON calendar_block(blocked_date);
CREATE INDEX idx_rate_override_dates ON rate_override(start_date, end_date);