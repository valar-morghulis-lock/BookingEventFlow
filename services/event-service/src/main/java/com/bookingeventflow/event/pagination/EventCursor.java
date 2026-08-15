package com.bookingeventflow.event.pagination;

import java.time.Instant;
import java.util.UUID;

public record EventCursor(
        Instant scheduledAt,
        UUID eventId
) {
}