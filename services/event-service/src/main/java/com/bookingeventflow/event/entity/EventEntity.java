package com.bookingeventflow.event.entity;

import com.bookingeventflow.event.domain.model.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @Column(
            name = "name",
            nullable = false
    )
    private String name;

    @Column(
            name = "description"
    )
    private String description;

    @Column(
            name = "scheduled_at",
            nullable = false
    )
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private EventStatus status;

    protected EventEntity() {
        // Required by JPA
    }

    /**
     * Constructor for NEW entities.
     *
     * ID is intentionally absent.
     * Hibernate generates it.
     *
     * Version is intentionally absent.
     * Hibernate manages it through @Version.
     */
    public EventEntity(
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {
        this.name = name;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.status = status;
    }

    public UUID id() {
        return id;
    }

    public Long version() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}