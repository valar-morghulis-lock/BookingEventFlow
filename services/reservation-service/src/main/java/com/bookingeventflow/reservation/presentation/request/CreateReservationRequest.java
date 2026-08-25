package com.bookingeventflow.reservation.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to hold seats for an event on behalf of a customer.")
public record CreateReservationRequest(

        @Schema(
                description = "Identifier of the customer requesting the hold.",
                example = "3f9a2b10-...-4c1e"
        )
        @NotNull(message = "Customer id must not be null")
        UUID customerId,

        @Schema(
                description = "Seat numbers to hold, e.g. R001-S01.",
                example = "[\"R001-S01\", \"R001-S02\"]"
        )
        @NotEmpty(message = "At least one seat number must be provided")
        @Size(max = 20, message = "Cannot request more than 20 seats per reservation")
        List<String> seatNumbers
) {
}