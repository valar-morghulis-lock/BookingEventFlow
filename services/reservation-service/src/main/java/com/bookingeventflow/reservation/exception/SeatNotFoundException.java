package com.bookingeventflow.reservation.exception;

import java.util.List;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(List<String> missingSeatNumbers) {
        super("Seats not found for this event: " + missingSeatNumbers);
    }
}