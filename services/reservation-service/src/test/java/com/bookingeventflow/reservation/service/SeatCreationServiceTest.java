package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.messaging.dto.EventPublishedMessage;
import com.bookingeventflow.reservation.observability.metrics.ReservationMetrics;
import com.bookingeventflow.reservation.repository.ProcessedEventRepository;
import com.bookingeventflow.reservation.repository.ReservationInventoryRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatCreationService")
class SeatCreationServiceTest {

    @Mock
    private ReservationInventoryRepository inventoryRepository;

    @Mock
    private ReservationSeatRepository seatRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private SimpleMeterRegistry meterRegistry;
    private ReservationMetrics reservationMetrics;
    private SeatCreationService seatCreationService;

    @BeforeEach
    void setUp() {

        meterRegistry = new SimpleMeterRegistry();
        reservationMetrics = new ReservationMetrics(meterRegistry);

        seatCreationService = new SeatCreationService(
                inventoryRepository,
                seatRepository,
                processedEventRepository,
                reservationMetrics
        );
    }

    @Test
    @DisplayName("should skip when event already processed")
    void shouldSkipWhenEventAlreadyProcessed() {

        EventPublishedMessage message = message(3, 10);

        when(processedEventRepository.existsById(message.eventId()))
                .thenReturn(true);

        seatCreationService.handle(message);

        verify(inventoryRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());

        assertMetric("skipped_already_processed", 1.0);
    }

    @Test
    @DisplayName("should create inventory and correct number of seats")
    void shouldCreateInventoryAndSeats() {

        EventPublishedMessage message = message(3, 10);

        when(processedEventRepository.existsById(message.eventId()))
                .thenReturn(false);

        when(inventoryRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seatCreationService.handle(message);

        verify(inventoryRepository).save(any());

        ArgumentCaptor<List> seatsCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(seatsCaptor.capture());

        assertEquals(30, seatsCaptor.getValue().size());

        verify(processedEventRepository).save(any());

        assertMetric("created", 1.0);
    }

    @Test
    @DisplayName("should generate correctly formatted seat numbers")
    void shouldGenerateCorrectSeatNumbers() {

        EventPublishedMessage message = message(2, 3);

        when(processedEventRepository.existsById(message.eventId()))
                .thenReturn(false);

        when(inventoryRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        seatCreationService.handle(message);

        ArgumentCaptor<List<ReservationSeatEntity>> seatsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(seatRepository).saveAll(seatsCaptor.capture());

        List<ReservationSeatEntity> seats = seatsCaptor.getValue();

        assertEquals(6, seats.size());

        assertEquals("R001-S01", seats.get(0).getSeatNumber());
        assertEquals("R001-S02", seats.get(1).getSeatNumber());
        assertEquals("R001-S03", seats.get(2).getSeatNumber());
        assertEquals("R002-S01", seats.get(3).getSeatNumber());
        assertEquals("R002-S02", seats.get(4).getSeatNumber());
        assertEquals("R002-S03", seats.get(5).getSeatNumber());
    }

    private EventPublishedMessage message(int numberOfRows, int seatsPerRow) {
        return new EventPublishedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                numberOfRows,
                seatsPerRow
        );
    }

    private void assertMetric(String result, double expectedCount) {
        assertEquals(
                expectedCount,
                meterRegistry.get("reservation.seat_creation")
                        .tags("result", result)
                        .counter()
                        .count()
        );
    }
}