package com.bookingeventflow.common.exception;

public enum ErrorCode {

    // ---------- Generic ----------
    UNKNOWN_ERROR,
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,
    FORBIDDEN,
    INTERNAL_ERROR,

    // ---------- Event Service ----------
    EVENT_NOT_FOUND,
    EVENT_ALREADY_EXISTS,

    CATEGORY_NOT_FOUND,
    CATEGORY_ALREADY_EXISTS,

    VENUE_NOT_FOUND,

    SEAT_SECTION_NOT_FOUND,
    SEAT_NOT_FOUND,
    SEAT_ALREADY_RESERVED,

    // ---------- Reservation ----------
    RESERVATION_NOT_FOUND,
    RESERVATION_EXPIRED,
    RESERVATION_ALREADY_CONFIRMED,

    // ---------- Booking ----------
    BOOKING_NOT_FOUND,
    BOOKING_ALREADY_CONFIRMED,

    // ---------- Payment ----------
    PAYMENT_FAILED,
    PAYMENT_TIMEOUT,

    // ---------- Customer ----------
    CUSTOMER_NOT_FOUND,
    CUSTOMER_ALREADY_EXISTS

}