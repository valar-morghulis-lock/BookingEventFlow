package com.bookingeventflow.event.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Standard error response returned by the Event Service.")
public record ErrorResponse(

        @Schema(
                description = "Timestamp when the error occurred",
                example = "2026-08-13T15:54:51.023450800Z"
        )
        Instant timestamp,

        @Schema(
                description = "HTTP status code",
                example = "409"
        )
        int status,

        @Schema(
                description = "HTTP status reason",
                example = "Conflict"
        )
        String error,

        @Schema(
                description = "Human-readable error message",
                example = "Completed events cannot be cancelled"
        )
        String message,

        @Schema(
                description = "Request path that produced the error",
                example = "/api/v1/events/512c5d0f-a416-4430-9265-4069b1637964/cancel"
        )
        String path,

        @Schema(
                description = "Validation errors grouped by field"
        )
        Map<String, List<String>> errors
) {
}