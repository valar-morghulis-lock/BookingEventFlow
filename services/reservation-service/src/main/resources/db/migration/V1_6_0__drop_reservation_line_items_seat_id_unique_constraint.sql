ALTER TABLE reservation_line_items
    DROP CONSTRAINT uq_reservation_line_items_seat_id;


CREATE INDEX idx_reservation_line_items_seat_id
    ON reservation_line_items (seat_id);