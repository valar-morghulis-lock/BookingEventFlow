package com.bookingeventflow.event.outbox.mapper;

import com.bookingeventflow.common.event.DomainEvent;
import com.bookingeventflow.event.domain.event.EventPublished;
import com.bookingeventflow.event.outbox.entity.OutboxEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxEventMapperImpl implements OutboxEventMapper {

    private final ObjectMapper objectMapper;

    public OutboxEventMapperImpl(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity toEntity(
            DomainEvent domainEvent
    ) {

        if (domainEvent == null) {
            throw new IllegalArgumentException(
                    "Domain event must not be null"
            );
        }

        String payload =
                serializePayload(domainEvent);

        return new OutboxEventEntity(
                domainEvent.aggregateId(),
                resolveEventType(domainEvent),
                domainEvent.occurredAt(),
                payload,
                Instant.now()
        );
    }

    private String serializePayload(
            DomainEvent domainEvent
    ) {

        try {
            return objectMapper.writeValueAsString(
                    domainEvent
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to serialize domain event: " +
                            domainEvent.getClass().getSimpleName(),
                    exception
            );
        }
    }

    private String resolveEventType(
            DomainEvent domainEvent
    ) {

        if (domainEvent instanceof EventPublished) {
            return "EVENT_PUBLISHED";
        }

        throw new IllegalArgumentException(
                "Unsupported domain event type: " +
                        domainEvent.getClass().getName()
        );
    }
}