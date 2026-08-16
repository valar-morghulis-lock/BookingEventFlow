package com.bookingeventflow.event.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventMetrics {

    private static final String OPERATIONS = "events.operations";
    private static final String PAGINATION = "events.pagination";

    private final MeterRegistry registry;

    public EventMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordOperation(String operation, Result result) {
        registry.counter(
                OPERATIONS,
                "operation", operation,
                "result", result.label()
        ).increment();
    }

    public void recordCursorUsed() {
        registry.counter(PAGINATION, "type", "cursor_used").increment();
    }

    public void recordPageWithNext() {
        registry.counter(PAGINATION, "type", "has_next").increment();
    }

    public enum Result {
        SUCCESS("success"),
        NOT_FOUND("not_found"),
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