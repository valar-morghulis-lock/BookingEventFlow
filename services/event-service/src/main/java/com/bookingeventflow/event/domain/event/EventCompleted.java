package com.bookingeventflow.event.domain.event;

import com.bookingeventflow.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCompleted(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt
) implements DomainEvent {

    public EventCompleted(
            UUID aggregateId,
            Instant occurredAt
    ) {
        this(
                UUID.randomUUID(),
                aggregateId,
                occurredAt
        );
    }
}