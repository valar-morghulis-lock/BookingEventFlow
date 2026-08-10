package com.bookingeventflow.event.domain.event;

import com.bookingeventflow.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventCreated(
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
}