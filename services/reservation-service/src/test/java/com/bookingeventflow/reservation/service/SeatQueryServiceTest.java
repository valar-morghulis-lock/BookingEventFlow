package com.bookingeventflow.reservation.service;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import com.bookingeventflow.reservation.entity.ReservationSeatEntity;
import com.bookingeventflow.reservation.presentation.response.SeatResponse;
import com.bookingeventflow.reservation.repository.ReservationSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatQueryServiceTest {

    @Mock
    private ReservationSeatRepository reservationSeatRepository;

    @Test
    void shouldReturnAllSeatsWhenNoStatusFilter() {

        UUID eventId = UUID.randomUUID();

        ReservationSeatEntity seat =
                new ReservationSeatEntity(UUID.randomUUID(), eventId, "R001-S01", 1, 1);

        when(reservationSeatRepository.findByEventIdOrderByRowNumberAscSeatNumberInRowAsc(eventId))
                .thenReturn(List.of(seat));

        SeatQueryService service = new SeatQueryService(reservationSeatRepository);

        List<SeatResponse> result = service.getSeatsForEvent(eventId, null);

        assertEquals(1, result.size());
        assertEquals("R001-S01", result.get(0).seatNumber());
        assertEquals(SeatStatus.AVAILABLE, result.get(0).status());
    }

    @Test
    void shouldFilterByStatusWhenProvided() {

        UUID eventId = UUID.randomUUID();

        when(reservationSeatRepository.findByEventIdAndStatusOrderByRowNumberAscSeatNumberInRowAsc(
                eventId, SeatStatus.HELD
        )).thenReturn(List.of());

        SeatQueryService service = new SeatQueryService(reservationSeatRepository);

        List<SeatResponse> result = service.getSeatsForEvent(eventId, SeatStatus.HELD);

        assertEquals(0, result.size());
    }
}