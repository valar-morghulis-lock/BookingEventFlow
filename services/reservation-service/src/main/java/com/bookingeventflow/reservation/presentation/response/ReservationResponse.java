package com.bookingeventflow.reservation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.bookingeventflow.reservation.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "A created reservation hold.")
public record ReservationResponse(

        @Schema(description = "Reservation identifier.")
        UUID reservationId,

        @Schema(description = "Event this reservation belongs to.")
        UUID eventId,

        @Schema(description = "Current status of the reservation.")
        ReservationStatus status,

        @Schema(description = "When this hold expires, if still pending.")
        Instant expiresAt,

        @Schema(description = "Seat numbers included in this reservation.")
        List<String> seats
) {
}