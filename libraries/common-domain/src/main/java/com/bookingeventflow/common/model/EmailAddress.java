package com.bookingeventflow.common.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            );

    public EmailAddress {
        Objects.requireNonNull(value, "email must not be null");

        value = value.trim().toLowerCase();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "email must not be blank"
            );
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid email address"
            );
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }

}