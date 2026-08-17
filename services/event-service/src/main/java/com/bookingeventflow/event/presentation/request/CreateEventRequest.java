package com.bookingeventflow.event.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;

@Schema(description = "Request used to create a new event.")
public record CreateEventRequest(

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
                example = "A conference covering modern Spring Boot development.",
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
        Instant scheduledAt,

        @Schema(
                description = "Number of seating rows. Each row contains exactly 10 seats.",
                example = "50",
                minimum = "1"
        )
        @NotNull(message = "Number of rows must not be null")
        @Min(value = 1, message = "Number of rows must be at least 1")
        Integer numberOfRows
) {
}