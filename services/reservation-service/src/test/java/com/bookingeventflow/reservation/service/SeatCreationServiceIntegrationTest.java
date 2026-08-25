package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.repository.ProcessedEventRepository;
import com.bookingeventflow.reservation.repository.ReservationInventoryRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class SeatCreationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private SeatCreationService seatCreationService;

    @Autowired
    private ReservationInventoryRepository inventoryRepository;

    @Autowired
    private ReservationSeatRepository seatRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldCreateInventoryAndSeatsInRealDatabase() {

        EventPublishedMessage message =
                createEventPublishedMessage(
                        2,
                        10
                );

        seatCreationService.handle(message);

        assertTrue(
                inventoryRepository.existsByEventId(
                        message.aggregateId()
                )
        );

        assertEquals(
                1,
                inventoryRepository.countByEventId(
                        message.aggregateId()
                )
        );

        assertEquals(
                20,
                seatRepository.countByEventId(
                        message.aggregateId()
                )
        );

        assertTrue(
                processedEventRepository.existsById(
                        message.eventId()
                )
        );
    }

    @Test
    void shouldBeIdempotentOnRedeliveredMessage() {

        EventPublishedMessage message =
                createEventPublishedMessage(
                        1,
                        10
                );

        seatCreationService.handle(message);

        seatCreationService.handle(message);

        assertEquals(
                1,
                inventoryRepository.countByEventId(
                        message.aggregateId()
                )
        );

        assertEquals(
                10,
                seatRepository.countByEventId(
                        message.aggregateId()
                )
        );

        assertTrue(
                processedEventRepository.existsById(
                        message.eventId()
                )
        );
    }

    private EventPublishedMessage createEventPublishedMessage(
            int numberOfRows,
            int seatsPerRow
    ) {
        return new EventPublishedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                numberOfRows,
                seatsPerRow
        );
    }
}