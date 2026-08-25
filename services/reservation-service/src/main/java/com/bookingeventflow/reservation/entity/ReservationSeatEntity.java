package com.bookingeventflow.reservation.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import com.bookingeventflow.reservation.domain.model.SeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "reservation_seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reservation_seats_event_seat_number",
                        columnNames = {"event_id", "seat_number"}
                ),
                @UniqueConstraint(
                        name = "uq_reservation_seats_inventory_row_seat",
                        columnNames = {"inventory_id", "row_number", "seat_number_in_row"}
                )
        }
)
public class ReservationSeatEntity extends AuditableEntity {

    @Column(
            name = "inventory_id",
            nullable = false,
            updatable = false
    )
    private UUID inventoryId;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "seat_number",
            nullable = false,
            updatable = false,
            length = 20
    )
    private String seatNumber;

    @Column(
            name = "row_number",
            nullable = false,
            updatable = false
    )
    private int rowNumber;

    @Column(
            name = "seat_number_in_row",
            nullable = false,
            updatable = false
    )
    private int seatNumberInRow;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private SeatStatus status;

    /**
     * Required by JPA.
     */
    protected ReservationSeatEntity() {
    }

    /**
     * Creates a new entity.
     */
    public ReservationSeatEntity(
            UUID inventoryId,
            UUID eventId,
            String seatNumber,
            int rowNumber,
            int seatNumberInRow
    ) {
        this.inventoryId = inventoryId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.seatNumberInRow = seatNumberInRow;
        this.status = SeatStatus.AVAILABLE;
    }

    public UUID getInventoryId() {
        return inventoryId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public int getSeatNumberInRow() {
        return seatNumberInRow;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}