package com.bookingeventflow.reservation.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationInventoryEntityTest {

    @Test
    void shouldComputeCapacityFromRowsAndSeatsPerRow() {

        UUID eventId = UUID.randomUUID();

        ReservationInventoryEntity inventory =
                new ReservationInventoryEntity(eventId, 3, 10);

        assertEquals(30, inventory.getCapacity());
        assertNotNull(inventory.id());
    }
}