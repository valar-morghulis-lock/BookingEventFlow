package com.bookingeventflow.event.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import com.bookingeventflow.event.domain.model.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity extends AuditableEntity {

    @Column(
            name = "name",
            nullable = false
    )
    private String name;

    @Column(name = "description")
    private String description;

    @Column(
            name = "scheduled_at",
            nullable = false
    )
    private Instant scheduledAt;

    @Column(
            name = "number_of_rows",
            nullable = false
    )
    private int numberOfRows;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private EventStatus status;

    /**
     * Required by JPA.
     */
    protected EventEntity() {
    }

    /**
     * Creates a new entity.
     */
    public EventEntity(
            String name,
            String description,
            Instant scheduledAt,
            int numberOfRows,
            EventStatus status
    ) {
        this.name = name;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.numberOfRows = numberOfRows;
        this.status = status;
    }

    /**
     * Reconstructs an existing persisted entity.
     */
    public EventEntity(
            UUID id,
            Long version,
            String name,
            String description,
            Instant scheduledAt,
            int numberOfRows,
            EventStatus status
    ) {
        this.name = name;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.numberOfRows = numberOfRows;
        this.status = status;

        reconstituteIdentity(
                id,
                version
        );
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

    public int getNumberOfRows() {
        return numberOfRows;
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

    public void setNumberOfRows(int numberOfRows) {
        this.numberOfRows = numberOfRows;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}