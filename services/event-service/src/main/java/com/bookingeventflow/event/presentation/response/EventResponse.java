package com.bookingeventflow.event.presentation.response;

import com.bookingeventflow.event.domain.model.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        Long version,
        String name,
        String description,
        Instant scheduledAt,
        EventStatus status
) {
}