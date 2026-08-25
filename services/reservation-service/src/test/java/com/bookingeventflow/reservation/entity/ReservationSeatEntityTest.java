package com.bookingeventflow.reservation.entity;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReservationSeatEntityTest {

    @Test
    void shouldStoreProvidedFieldsAndDefaultToAvailable() {

        UUID inventoryId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ReservationSeatEntity seat =
                new ReservationSeatEntity(
                        inventoryId,
                        eventId,
                        "R001-S01",
                        1,
                        1
                );

        assertEquals(inventoryId, seat.getInventoryId());
        assertEquals(eventId, seat.getEventId());
        assertEquals("R001-S01", seat.getSeatNumber());
        assertEquals(1, seat.getRowNumber());
        assertEquals(1, seat.getSeatNumberInRow());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void shouldGenerateIdOnConstruction() {

        ReservationSeatEntity seat =
                new ReservationSeatEntity(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "R002-S05",
                        2,
                        5
                );

        assertNotNull(seat.id());
    }

    @Test
    void shouldAllowStatusTransitionViaSetter() {

        ReservationSeatEntity seat =
                new ReservationSeatEntity(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "R001-S01",
                        1,
                        1
                );

        seat.setStatus(SeatStatus.HELD);

        assertEquals(SeatStatus.HELD, seat.getStatus());
    }
}