package com.bookingeventflow.reservation.exception;

import com.bookingeventflow.reservation.domain.model.ReservationStatus;

import java.util.UUID;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(
            UUID reservationId,
            ReservationStatus currentStatus
    ) {
        super(
                "Reservation " + reservationId
                        + " is in state " + currentStatus
                        + " and cannot be transitioned"
        );
    }
}