package com.bookingeventflow.event.domain.event;

import com.bookingeventflow.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCreated(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt,
        int numberOfRows,
        int seatsPerRow
) implements DomainEvent {

    public EventCreated(
            UUID aggregateId,
            Instant occurredAt,
            int numberOfRows,
            int seatsPerRow
    ) {
        this(
                UUID.randomUUID(),
                aggregateId,
                occurredAt,
                numberOfRows,
                seatsPerRow
        );
    }
}