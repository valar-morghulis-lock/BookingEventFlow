package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.entity.ReservationEntity;
import com.bookingeventflow.reservation.entity.ReservationLineItemEntity;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.exception.InvalidReservationStateException;
import com.bookingeventflow.reservation.exception.ReservationNotFoundException;
import com.bookingeventflow.reservation.exception.SeatNotFoundException;
import com.bookingeventflow.reservation.exception.SeatUnavailableException;
import com.bookingeventflow.reservation.observability.metrics.ReservationMetrics;
import com.bookingeventflow.reservation.presentation.request.CreateReservationRequest;
import com.bookingeventflow.reservation.presentation.response.ReservationResponse;
import com.bookingeventflow.reservation.repository.ReservationLineItemRepository;
import com.bookingeventflow.reservation.repository.ReservationRepository;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationLineItemRepository reservationLineItemRepository;

    @Mock
    private ReservationSeatRepository reservationSeatRepository;

    private SimpleMeterRegistry meterRegistry;
    private ReservationMetrics reservationMetrics;
    private ReservationService reservationService;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        meterRegistry = new SimpleMeterRegistry();
        reservationMetrics = new ReservationMetrics(meterRegistry);

        reservationService = new ReservationService(
                reservationRepository,
                reservationLineItemRepository,
                reservationSeatRepository,
                reservationMetrics,
                10
        );
    }

    @Nested
    @DisplayName("createReservation")
    class CreateReservationTests {

        @Test
        @DisplayName("should hold seats and create pending reservation")
        void shouldHoldSeatsAndCreateReservation() {

            CreateReservationRequest request =
                    new CreateReservationRequest(CUSTOMER_ID, List.of("R001-S01", "R001-S02"));

            ReservationSeatEntity seat1 = seat("R001-S01", SeatStatus.AVAILABLE);
            ReservationSeatEntity seat2 = seat("R001-S02", SeatStatus.AVAILABLE);

            when(reservationSeatRepository.lockSeatsForUpdate(EVENT_ID, request.seatNumbers()))
                    .thenReturn(List.of(seat1, seat2));

            when(reservationRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponse response =
                    reservationService.createReservation(EVENT_ID, request);

            assertEquals(ReservationStatus.PENDING, response.status());
            assertEquals(2, response.seats().size());
            assertEquals(SeatStatus.HELD, seat1.getStatus());
            assertEquals(SeatStatus.HELD, seat2.getStatus());

            assertMetric("create_reservation", "success", 1.0);
        }

        @Test
        @DisplayName("should throw when a seat is not found for the event")
        void shouldThrowWhenSeatNotFound() {

            CreateReservationRequest request =
                    new CreateReservationRequest(CUSTOMER_ID, List.of("R099-S99"));

            when(reservationSeatRepository.lockSeatsForUpdate(EVENT_ID, request.seatNumbers()))
                    .thenReturn(List.of());

            assertThrows(
                    SeatNotFoundException.class,
                    () -> reservationService.createReservation(EVENT_ID, request)
            );

            assertMetric("create_reservation", "not_found", 1.0);
        }

        @Test
        @DisplayName("should throw when a seat is already held")
        void shouldThrowWhenSeatUnavailable() {

            CreateReservationRequest request =
                    new CreateReservationRequest(CUSTOMER_ID, List.of("R001-S01"));

            when(reservationSeatRepository.lockSeatsForUpdate(EVENT_ID, request.seatNumbers()))
                    .thenReturn(List.of(seat("R001-S01", SeatStatus.HELD)));

            assertThrows(
                    SeatUnavailableException.class,
                    () -> reservationService.createReservation(EVENT_ID, request)
            );

            assertMetric("create_reservation", "seat_unavailable", 1.0);
        }
    }

    @Nested
    @DisplayName("confirmReservation")
    class ConfirmReservationTests {

        @Test
        @DisplayName("should confirm a pending reservation and book its seats")
        void shouldConfirmPendingReservation() {

            UUID reservationId = UUID.randomUUID();

            ReservationEntity reservation =
                    new ReservationEntity(EVENT_ID, CUSTOMER_ID, Instant.now().plusSeconds(600));

            ReservationSeatEntity seat = seat("R001-S01", SeatStatus.HELD);

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(reservation));

            when(reservationLineItemRepository.findByReservationId(reservationId))
                    .thenReturn(List.of(
                            new ReservationLineItemEntity(reservationId, seat.id(), EVENT_ID, "R001-S01")
                    ));

            when(reservationSeatRepository.findAllById(anyList()))
                    .thenReturn(List.of(seat));

            ReservationResponse response = reservationService.confirmReservation(reservationId);

            assertEquals(ReservationStatus.CONFIRMED, response.status());
            assertEquals(SeatStatus.BOOKED, seat.getStatus());

            assertMetric("confirm_reservation", "success", 1.0);
        }

        @Test
        @DisplayName("should throw when reservation does not exist")
        void shouldThrowWhenNotFound() {

            UUID reservationId = UUID.randomUUID();

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    ReservationNotFoundException.class,
                    () -> reservationService.confirmReservation(reservationId)
            );

            assertMetric("confirm_reservation", "not_found", 1.0);
        }

        @Test
        @DisplayName("should throw when reservation is not pending")
        void shouldThrowWhenNotPending() {

            UUID reservationId = UUID.randomUUID();

            ReservationEntity reservation =
                    new ReservationEntity(EVENT_ID, CUSTOMER_ID, Instant.now());
            reservation.setStatus(ReservationStatus.CONFIRMED);

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(reservation));

            assertThrows(
                    InvalidReservationStateException.class,
                    () -> reservationService.confirmReservation(reservationId)
            );

            assertMetric("confirm_reservation", "invalid_state", 1.0);
        }
    }

    @Nested
    @DisplayName("releaseReservation")
    class ReleaseReservationTests {

        @Test
        @DisplayName("should release a pending reservation and free its seats")
        void shouldReleasePendingReservation() {

            UUID reservationId = UUID.randomUUID();

            ReservationEntity reservation =
                    new ReservationEntity(EVENT_ID, CUSTOMER_ID, Instant.now().plusSeconds(600));

            ReservationSeatEntity seat = seat("R001-S01", SeatStatus.HELD);

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(reservation));

            when(reservationLineItemRepository.findByReservationId(reservationId))
                    .thenReturn(List.of(
                            new ReservationLineItemEntity(reservationId, seat.id(), EVENT_ID, "R001-S01")
                    ));

            when(reservationSeatRepository.findAllById(anyList()))
                    .thenReturn(List.of(seat));

            ReservationResponse response = reservationService.releaseReservation(reservationId);

            assertEquals(ReservationStatus.RELEASED, response.status());
            assertEquals(SeatStatus.AVAILABLE, seat.getStatus());

            assertMetric("release_reservation", "success", 1.0);
        }
    }

    private ReservationSeatEntity seat(String seatNumber, SeatStatus status) {

        ReservationSeatEntity seat =
                new ReservationSeatEntity(UUID.randomUUID(), EVENT_ID, seatNumber, 1, 1);

        seat.setStatus(status);

        return seat;
    }

    private void assertMetric(String operation, String result, double expectedCount) {
        assertEquals(
                expectedCount,
                meterRegistry.get("reservation.operations")
                        .tags("operation", operation, "result", result)
                        .counter()
                        .count()
        );
    }


    @Nested
    @DisplayName("getReservation")
    class GetReservationTests {

        @Test
        @DisplayName("should return reservation when found")
        void shouldReturnReservationWhenFound() {

            UUID reservationId = UUID.randomUUID();

            ReservationEntity reservation =
                    new ReservationEntity(EVENT_ID, CUSTOMER_ID, Instant.now().plusSeconds(600));

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.of(reservation));

            when(reservationLineItemRepository.findByReservationId(reservationId))
                    .thenReturn(List.of());

            when(reservationSeatRepository.findAllById(anyList()))
                    .thenReturn(List.of());

            ReservationResponse response = reservationService.getReservation(reservationId);

            assertEquals(ReservationStatus.PENDING, response.status());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrowWhenNotFound() {

            UUID reservationId = UUID.randomUUID();

            when(reservationRepository.findById(reservationId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    ReservationNotFoundException.class,
                    () -> reservationService.getReservation(reservationId)
            );
        }
    }
}