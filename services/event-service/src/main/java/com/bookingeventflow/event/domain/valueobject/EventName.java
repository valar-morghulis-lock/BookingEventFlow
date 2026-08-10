package com.bookingeventflow.event.domain.valueobject;

import java.util.Objects;

public record EventName(String value) {

    private static final int MAX_LENGTH = 200;

    public EventName {
        Objects.requireNonNull(
                value,
                "Event name must not be null"
        );

        value = value.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Event name must not be blank"
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Event name must not exceed "
                            + MAX_LENGTH
                            + " characters"
            );
        }
    }

    public static EventName of(String value) {
        return new EventName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
