package com.bookingeventflow.event.presentation.response;

import com.bookingeventflow.event.domain.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Event resource returned by the Event Service.")
public record EventResponse(

        @Schema(
                description = "Unique identifier of the event.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Optimistic locking version of the event.",
                example = "1"
        )
        Long version,

        @Schema(
                description = "Name of the event.",
                example = "Spring Boot Conference 2026"
        )
        String name,

        @Schema(
                description = "Description of the event.",
                example = "A conference covering modern Spring Boot development."
        )
        String description,

        @Schema(
                description = "Scheduled start time of the event.",
                example = "2026-12-15T18:30:00Z"
        )
        Instant scheduledAt,

        @Schema(
                description = "Current lifecycle status of the event.",
                example = "PUBLISHED"
        )
        EventStatus status
) {
}