CREATE TABLE reservations (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    event_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,

    CONSTRAINT pk_reservations
        PRIMARY KEY (id)
);

CREATE INDEX idx_reservations_event_id
    ON reservations (event_id);

CREATE INDEX idx_reservations_customer_id
    ON reservations (customer_id);

CREATE INDEX idx_reservations_status_expires_at
    ON reservations (status, expires_at);