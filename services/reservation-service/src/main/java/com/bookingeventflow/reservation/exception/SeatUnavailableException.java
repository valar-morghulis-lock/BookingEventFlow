package com.bookingeventflow.reservation.exception;

import java.util.List;

public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(List<String> unavailableSeatNumbers) {
        super("Seats are not available: " + unavailableSeatNumbers);
    }
}