DROP INDEX IF EXISTS idx_events_status;

CREATE INDEX idx_events_status_scheduled_at_id
    ON events (status, scheduled_at, id);