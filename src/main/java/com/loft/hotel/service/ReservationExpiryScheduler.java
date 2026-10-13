package com.loft.hotel.service;

import com.loft.hotel.entity.Reservation;
import com.loft.hotel.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically sweeps and cancels abandoned reservations where the guest
 * started a booking but never completed payment, freeing up the locked calendar dates.
 */
@Service
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    // Timeout duration in minutes before an unpaid reservation is automatically released
    public static final int EXPIRY_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository,
                                      ReservationService reservationService) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    // Runs automatically every 60 seconds (1 minute)
    @Scheduled(fixedRate = 60000)
    public void sweepExpiredReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        List<Reservation> expiredList = reservationRepository.findPendingOlderThan(cutoff);

        if (!expiredList.isEmpty()) {
            log.info("Found {} abandoned PENDING reservations older than {} minutes. Releasing locked dates...",
                    expiredList.size(), EXPIRY_MINUTES);
            for (Reservation res : expiredList) {
                try {
                    reservationService.cancel(res.getReservationId());
                    log.info("Auto-cancelled expired reservation #{} and released calendar dates.", res.getReservationId());
                } catch (Exception e) {
                    log.error("Failed to auto-cancel expired reservation #{}: {}", res.getReservationId(), e.getMessage());
                }
            }
        }
    }
}
