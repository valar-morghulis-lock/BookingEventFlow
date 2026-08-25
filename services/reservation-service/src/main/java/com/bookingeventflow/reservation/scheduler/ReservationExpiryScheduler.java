package com.bookingeventflow.reservation.scheduler;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import com.bookingeventflow.reservation.entity.ReservationEntity;
import com.bookingeventflow.reservation.repository.ReservationRepository;
import com.bookingeventflow.reservation.service.ReservationService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public ReservationExpiryScheduler(
            ReservationRepository reservationRepository,
            ReservationService reservationService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${reservation.expiry-sweep-interval-ms:60000}")
    @SchedulerLock(
            name = "reservation-expiry-sweep",
            lockAtLeastFor = "PT10S",
            lockAtMostFor = "PT5M"
    )
    public void sweepExpiredReservations() {

        List<ReservationEntity> expired =
                reservationRepository.findByStatusAndExpiresAtBefore(
                        ReservationStatus.PENDING,
                        Instant.now()
                );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Sweeping {} expired reservations", expired.size());

        for (ReservationEntity reservation : expired) {

            try {
                reservationService.expireReservation(reservation.id());
            } catch (Exception exception) {

                log.error(
                        "Failed to expire reservation {}",
                        reservation.id(),
                        exception
                );
            }
        }
    }
}