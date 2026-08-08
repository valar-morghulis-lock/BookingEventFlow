package com.bookingeventflow.common.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        amount = amount.stripTrailingZeros();
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {

        requireSameCurrency(other);

        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {

        requireSameCurrency(other);

        BigDecimal result = amount.subtract(other.amount);

        if (result.signum() < 0) {
            throw new IllegalArgumentException("resulting amount must not be negative");
        }

        return new Money(result, currency);
    }

    public Money multiply(BigDecimal multiplier) {

        Objects.requireNonNull(multiplier, "multiplier must not be null");

        if (multiplier.signum() < 0) {
            throw new IllegalArgumentException("multiplier must not be negative");
        }

        return new Money(amount.multiply(multiplier), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {

        Objects.requireNonNull(other, "money must not be null");

        if (currency != other.currency) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
        }
    }

}