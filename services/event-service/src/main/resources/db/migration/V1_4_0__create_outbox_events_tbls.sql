CREATE TABLE outbox_events (
    id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_outbox_events
        PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_created_at
    ON outbox_events (created_at);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events (event_type);