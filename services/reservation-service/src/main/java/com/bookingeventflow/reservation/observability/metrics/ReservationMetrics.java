package com.bookingeventflow.reservation.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ReservationMetrics {

    private static final String SEAT_CREATION = "reservation.seat_creation";
    private static final String OPERATIONS = "reservation.operations";

    private final MeterRegistry registry;

    public ReservationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSeatCreation(SeatCreationResult result) {
        registry.counter(SEAT_CREATION, "result", result.label()).increment();
    }

    public void recordOperation(String operation, Result result) {
        registry.counter(
                OPERATIONS,
                "operation", operation,
                "result", result.label()
        ).increment();
    }

    public enum SeatCreationResult {
        CREATED("created"),
        SKIPPED_ALREADY_PROCESSED("skipped_already_processed"),
        ERROR("error");

        private final String label;

        SeatCreationResult(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Result {
        SUCCESS("success"),
        NOT_FOUND("not_found"),
        SEAT_UNAVAILABLE("seat_unavailable"),
        INVALID_STATE("invalid_state"),
        ERROR("error");

        private final String label;

        Result(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}