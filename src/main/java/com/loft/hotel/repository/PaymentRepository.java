package com.loft.hotel.repository;

import com.loft.hotel.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByReservationIdAndPaymentStatus(
            Integer reservationId,
            Payment.PaymentStatus paymentStatus
    );

    Optional<Payment> findByTransactionId(String transactionId);

    // Locks the payment row while processing payment notifications.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :id")
    Optional<Payment> findForUpdate(@Param("id") String id);
}
