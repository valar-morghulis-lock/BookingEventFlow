CREATE TABLE reservation_line_items (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    reservation_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    event_id UUID NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT pk_reservation_line_items
        PRIMARY KEY (id),

    CONSTRAINT fk_reservation_line_items_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations (id),

    CONSTRAINT fk_reservation_line_items_seat
        FOREIGN KEY (seat_id)
        REFERENCES reservation_seats (id),

    CONSTRAINT uq_reservation_line_items_seat_id
        UNIQUE (seat_id)
);

CREATE INDEX idx_reservation_line_items_reservation_id
    ON reservation_line_items (reservation_id);

CREATE INDEX idx_reservation_line_items_event_id
    ON reservation_line_items (event_id);