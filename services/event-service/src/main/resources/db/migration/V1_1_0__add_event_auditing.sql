ALTER TABLE events
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_by VARCHAR(100);

UPDATE events
SET
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    created_by = 'system',
    updated_by = 'system'
WHERE created_at IS NULL;

ALTER TABLE events
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL;