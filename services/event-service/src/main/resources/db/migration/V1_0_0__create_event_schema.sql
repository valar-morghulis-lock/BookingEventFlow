CREATE TABLE events (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,

    CONSTRAINT pk_events
        PRIMARY KEY (id),

    CONSTRAINT ck_events_status
        CHECK (status IN (
            'DRAFT',
            'PUBLISHED',
            'CANCELLED',
            'COMPLETED'
        ))
);

CREATE INDEX idx_events_status
    ON events (status);

CREATE INDEX idx_events_scheduled_at
    ON events (scheduled_at);