package com.loft.hotel.repository;

import com.loft.hotel.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByReservationIdAndPaymentStatus(
            Integer reservationId,
            Payment.PaymentStatus paymentStatus
    );

    Optional<Payment> findByTransactionId(
            String transactionId
    );
}