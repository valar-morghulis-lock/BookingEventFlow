package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.entity.ProcessedEventEntity;
import com.bookingeventflow.reservation.entity.ReservationInventoryEntity;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.observability.metrics.ReservationMetrics;
import com.bookingeventflow.reservation.repository.ProcessedEventRepository;
import com.bookingeventflow.reservation.repository.ReservationInventoryRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SeatCreationService {

    private static final Logger log =
            LoggerFactory.getLogger(SeatCreationService.class);

    private static final String EVENT_TYPE = "EVENT_PUBLISHED";

    private final ReservationInventoryRepository inventoryRepository;
    private final ReservationSeatRepository seatRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ReservationMetrics reservationMetrics;

    public SeatCreationService(
            ReservationInventoryRepository inventoryRepository,
            ReservationSeatRepository seatRepository,
            ProcessedEventRepository processedEventRepository,
            ReservationMetrics reservationMetrics
    ) {
        this.inventoryRepository = inventoryRepository;
        this.seatRepository = seatRepository;
        this.processedEventRepository = processedEventRepository;
        this.reservationMetrics = reservationMetrics;
    }

    /**
     * Creates reservation inventory and seats for a published event,
     * guarded by an idempotency check against processed_events. Safe
     * to call multiple times with the same message (e.g., on Kafka
     * redelivery) — only the first call has any effect.
     */
    @Timed(
            value = "reservation.seat_creation.duration",
            description = "Seat creation duration for a published event"
    )
    @Transactional
    public void handle(EventPublishedMessage message) {

        if (processedEventRepository.existsById(message.eventId())) {

            log.debug(
                    "Skipping already-processed event {}",
                    message.eventId()
            );

            reservationMetrics.recordSeatCreation(ReservationMetrics.SeatCreationResult.SKIPPED_ALREADY_PROCESSED);

            return;
        }

        ReservationInventoryEntity inventory =
                inventoryRepository.save(
                        new ReservationInventoryEntity(
                                message.aggregateId(),
                                message.numberOfRows(),
                                message.seatsPerRow()
                        )
                );

        List<ReservationSeatEntity> seats =
                buildSeats(
                        inventory.id(),
                        message.aggregateId(),
                        message.numberOfRows(),
                        message.seatsPerRow()
                );

        seatRepository.saveAll(seats);

        processedEventRepository.save(
                new ProcessedEventEntity(
                        message.eventId(),
                        EVENT_TYPE
                )
        );

        reservationMetrics.recordSeatCreation(ReservationMetrics.SeatCreationResult.CREATED);


        log.info(
                "Created inventory {} with {} seats for event {}",
                inventory.id(),
                seats.size(),
                message.aggregateId()
        );
    }

    private List<ReservationSeatEntity> buildSeats(
            UUID inventoryId,
            UUID eventId,
            int numberOfRows,
            int seatsPerRow
    ) {

        List<ReservationSeatEntity> seats =
                new ArrayList<>(numberOfRows * seatsPerRow);

        for (int row = 1; row <= numberOfRows; row++) {
            for (int seatInRow = 1; seatInRow <= seatsPerRow; seatInRow++) {

                seats.add(
                        new ReservationSeatEntity(
                                inventoryId,
                                eventId,
                                formatSeatNumber(row, seatInRow),
                                row,
                                seatInRow
                        )
                );
            }
        }

        return seats;
    }

    private String formatSeatNumber(int row, int seatInRow) {
        return String.format("R%03d-S%02d", row, seatInRow);
    }
}