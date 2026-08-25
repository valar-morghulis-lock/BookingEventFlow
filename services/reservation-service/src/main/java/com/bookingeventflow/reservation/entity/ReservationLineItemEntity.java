package com.bookingeventflow.reservation.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "reservation_line_items")
public class ReservationLineItemEntity extends AuditableEntity {

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "seat_number", nullable = false, updatable = false, length = 20)
    private String seatNumber;

    protected ReservationLineItemEntity() {
    }

    public ReservationLineItemEntity(
            UUID reservationId,
            UUID seatId,
            UUID eventId,
            String seatNumber
    ) {
        this.reservationId = reservationId;
        this.seatId = seatId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}