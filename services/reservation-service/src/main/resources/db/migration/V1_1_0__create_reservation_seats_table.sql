CREATE TABLE reservation_seats (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    inventory_id UUID NOT NULL,
    event_id UUID NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    row_number INTEGER NOT NULL,
    seat_number_in_row INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT pk_reservation_seats
        PRIMARY KEY (id),

    CONSTRAINT fk_reservation_seats_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES reservation_inventory (id),

    CONSTRAINT uq_reservation_seats_event_seat_number
        UNIQUE (event_id, seat_number),

    CONSTRAINT uq_reservation_seats_inventory_row_seat
        UNIQUE (inventory_id, row_number, seat_number_in_row)
);

CREATE INDEX idx_reservation_seats_event_id
    ON reservation_seats (event_id);

CREATE INDEX idx_reservation_seats_inventory_id
    ON reservation_seats (inventory_id);