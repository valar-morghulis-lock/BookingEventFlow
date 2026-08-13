package com.bookingeventflow.event.presentation.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateEventRequest(

        @NotBlank(message = "Name must not be blank")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Scheduled time must not be null")
        @Future(message = "Scheduled time must be in the future")
        Instant scheduledAt
) {
}