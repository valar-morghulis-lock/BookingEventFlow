ALTER TABLE events
    ADD COLUMN seating_type VARCHAR(30) NOT NULL DEFAULT 'RESERVED_SEATING',
    ADD COLUMN number_of_rows INTEGER NOT NULL DEFAULT 1;

ALTER TABLE events
    ADD CONSTRAINT ck_events_seating_type
        CHECK (seating_type IN ('RESERVED_SEATING'));

ALTER TABLE events
    ADD CONSTRAINT ck_events_number_of_rows
        CHECK (number_of_rows > 0);