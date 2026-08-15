package com.bookingeventflow.event.domain.event;

import com.bookingeventflow.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventPublished(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt
) implements DomainEvent {

    public EventPublished(
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