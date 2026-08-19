package com.bookingeventflow.event.outbox.entity;

import com.bookingeventflow.common.identifier.UUIDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "aggregate_id",
            nullable = false,
            updatable = false
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false
    )
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String payload;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected OutboxEventEntity() {
        // Required by JPA.
    }

    public OutboxEventEntity(
            UUID aggregateId,
            String eventType,
            Instant occurredAt,
            String payload,
            Instant createdAt
    ) {
        this.id = UUIDGenerator.INSTANCE.generate();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}