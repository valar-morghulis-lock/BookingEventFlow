package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.entity.ReservationInventoryEntity;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.exception.SeatUnavailableException;
import com.bookingeventflow.reservation.presentation.request.CreateReservationRequest;
import com.bookingeventflow.reservation.presentation.response.ReservationResponse;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class ReservationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationInventoryRepository inventoryRepository;

    @Autowired
    private ReservationSeatRepository seatRepository;

    @Test
    void shouldConfirmAndReleaseReservation() {

        UUID eventId = UUID.randomUUID();
        seedSingleSeat(eventId);

        ReservationResponse created = reservationService.createReservation(
                eventId,
                new CreateReservationRequest(UUID.randomUUID(), List.of("R001-S01"))
        );

        ReservationResponse confirmed = reservationService.confirmReservation(created.reservationId());
        assertEquals("CONFIRMED", confirmed.status().name());
    }

    @Test
    void onlyOneConcurrentRequestShouldWinTheSameSeat() throws InterruptedException {

        UUID eventId = UUID.randomUUID();
        seedSingleSeat(eventId);

        int attempts = 5;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch latch = new CountDownLatch(attempts);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    reservationService.createReservation(
                            eventId,
                            new CreateReservationRequest(UUID.randomUUID(), List.of("R001-S01"))
                    );
                    successCount.incrementAndGet();
                } catch (SeatUnavailableException exception) {
                    conflictCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get());
        assertEquals(attempts - 1, conflictCount.get());
    }

    private void seedSingleSeat(UUID eventId) {

        ReservationInventoryEntity inventory =
                inventoryRepository.save(new ReservationInventoryEntity(eventId, 1, 1));

        seatRepository.save(
                new ReservationSeatEntity(inventory.id(), eventId, "R001-S01", 1, 1)
        );
    }
}