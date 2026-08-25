package com.bookingeventflow.reservation.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import com.bookingeventflow.reservation.domain.model.InventoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "reservation_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reservation_inventory_event_id",
                columnNames = "event_id"
        )
)
public class ReservationInventoryEntity extends AuditableEntity {

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "number_of_rows",
            nullable = false,
            updatable = false
    )
    private int numberOfRows;

    @Column(
            name = "seats_per_row",
            nullable = false,
            updatable = false
    )
    private int seatsPerRow;

    @Column(
            nullable = false,
            updatable = false
    )
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private InventoryStatus status;

    /**
     * Required by JPA.
     */
    protected ReservationInventoryEntity() {
    }

    /**
     * Creates a new entity.
     */
    public ReservationInventoryEntity(
            UUID eventId,
            int numberOfRows,
            int seatsPerRow
    ) {
        this.eventId = eventId;
        this.numberOfRows = numberOfRows;
        this.seatsPerRow = seatsPerRow;
        this.capacity = numberOfRows * seatsPerRow;
        this.status = InventoryStatus.READY;
    }

    public UUID getEventId() {
        return eventId;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public int getCapacity() {
        return capacity;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }
}