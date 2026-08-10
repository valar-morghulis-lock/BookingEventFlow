package com.bookingeventflow.event.domain.valueobject;

public record EventDescription(String value) {

    private static final int MAX_LENGTH = 2000;

    public EventDescription {
        value = normalize(value);
    }

    public static EventDescription of(String value) {
        return new EventDescription(value);
    }

    private static String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Event description must not exceed "
                            + MAX_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public String toString() {
        return value == null ? "" : value;
    }
}
