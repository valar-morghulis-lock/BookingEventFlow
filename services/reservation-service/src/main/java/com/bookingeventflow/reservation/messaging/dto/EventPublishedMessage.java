package com.bookingeventflow.reservation.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Deserialization target for the EVENT_PUBLISHED message produced by
 * event-service's outbox and forwarded by Debezium. Field names must
 * match event-service's EventPublished domain event exactly, since
 * Jackson serializes that record directly with no field renaming.
 */
public record EventPublishedMessage(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt,
        int numberOfRows,
        int seatsPerRow
) {
}