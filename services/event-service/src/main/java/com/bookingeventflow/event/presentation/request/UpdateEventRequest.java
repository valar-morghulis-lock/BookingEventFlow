package com.bookingeventflow.event.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request used to update an existing event.")
public record UpdateEventRequest(

        @Schema(
                description = "Name of the event.",
                example = "Spring Boot Conference 2026",
                maxLength = 200
        )
        @NotBlank(message = "Name must not be blank")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Schema(
                description = "Optional description of the event.",
                example = "Updated conference description.",
                maxLength = 2000
        )
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Schema(
                description = "Scheduled start time of the event.",
                example = "2026-12-15T18:30:00Z"
        )
        @NotNull(message = "Scheduled time must not be null")
        @Future(message = "Scheduled time must be in the future")
        Instant scheduledAt
) {
}