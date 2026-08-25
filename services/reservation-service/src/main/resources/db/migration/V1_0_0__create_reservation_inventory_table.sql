CREATE TABLE reservation_inventory (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    event_id UUID NOT NULL,
    number_of_rows INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT pk_reservation_inventory
        PRIMARY KEY (id),

    CONSTRAINT uq_reservation_inventory_event_id
        UNIQUE (event_id)
);