package com.bookingeventflow.event.domain.event;

import com.bookingeventflow.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventPublished(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt,
        int numberOfRows,
        int seatsPerRow
) implements DomainEvent {

    public EventPublished(
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