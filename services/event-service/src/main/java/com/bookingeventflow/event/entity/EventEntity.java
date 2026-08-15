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
     * Creates a NEW entity.
     *
     * BaseEntity generates the UUID.
     * Hibernate manages the @Version field.
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

    /**
     * Reconstructs an EXISTING persisted entity.
     *
     * This constructor is intentionally different from the
     * new-entity constructor. It allows infrastructure/tests
     * to represent an entity that already exists in the database.
     */
    public EventEntity(
            UUID id,
            Long version,
            String name,
            String description,
            Instant scheduledAt,
            EventStatus status
    ) {
        this.name = name;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.status = status;

        reconstituteIdentity(id, version);
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