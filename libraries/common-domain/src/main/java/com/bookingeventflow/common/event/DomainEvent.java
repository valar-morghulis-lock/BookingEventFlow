package com.bookingeventflow.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    UUID aggregateId();

    Instant occurredAt();
}