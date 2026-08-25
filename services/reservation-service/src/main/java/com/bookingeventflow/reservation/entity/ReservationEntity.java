package com.bookingeventflow.reservation.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import com.bookingeventflow.reservation.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationEntity extends AuditableEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ReservationEntity() {
    }

    public ReservationEntity(
            UUID eventId,
            UUID customerId,
            Instant expiresAt
    ) {
        this.eventId = eventId;
        this.customerId = customerId;
        this.status = ReservationStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}