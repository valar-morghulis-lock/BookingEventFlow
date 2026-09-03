package com.bookingeventflow.customer.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomerMetrics {

    private static final String OPERATIONS = "customer.operations";

    private final MeterRegistry registry;

    public CustomerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordOperation(String operation, Result result) {
        registry.counter(
                OPERATIONS,
                "operation", operation,
                "result", result.label()
        ).increment();
    }

    public enum Result {
        SUCCESS("success"),
        EMAIL_TAKEN("email_taken"),
        INVALID_CREDENTIALS("invalid_credentials"),
        NOT_FOUND("not_found");

        private final String label;

        Result(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}