package com.bookingeventflow.reservation.presentation.response;

import com.bookingeventflow.reservation.domain.model.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single seat's current state.")
public record SeatResponse(
        String seatNumber,
        int rowNumber,
        int seatNumberInRow,
        SeatStatus status
) {
}