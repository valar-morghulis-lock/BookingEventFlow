package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.observability.metrics.ReservationMetrics;
import com.bookingeventflow.reservation.repository.ProcessedEventRepository;
import com.bookingeventflow.reservation.repository.ReservationInventoryRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SeatBatchInsertIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SeatCreationService seatCreationService;

    @Autowired
    private ReservationSeatRepository seatRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldBatchInsertsWhenCreatingManySeats() {

        Statistics statistics =
                entityManagerFactory
                        .unwrap(SessionFactory.class)
                        .getStatistics();

        statistics.clear();

        EventPublishedMessage message = new EventPublishedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                50,   // numberOfRows
                10    // seatsPerRow -> 500 seats total
        );

        seatCreationService.handle(message);

        assertEquals(500, seatRepository.count());

        long insertCount = statistics.getEntityInsertCount();
        long prepareStatementCount = statistics.getPrepareStatementCount();

        assertEquals(502, insertCount); // 500 seats + 1 inventory row

        // With batch_size=50, a working batch config prepares far fewer
        // statements than the row count — a broken/absent batch config
        // would prepare close to one statement per insert.
        assertTrue(
                prepareStatementCount < insertCount / 2,
                "Expected batching to reduce prepared statement count; got "
                        + prepareStatementCount
                        + " statements for "
                        + insertCount
                        + " inserts"
        );
    }
}