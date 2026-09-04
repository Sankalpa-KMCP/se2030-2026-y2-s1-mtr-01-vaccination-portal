-- Enforce MF-02 TimeSlot integrity invariants at the MySQL persistence boundary.

ALTER TABLE time_slots
    ADD CONSTRAINT chk_time_slots_capacity_positive CHECK (capacity > 0),
    ADD CONSTRAINT chk_time_slots_reserved_nonnegative CHECK (reserved_count >= 0),
    ADD CONSTRAINT chk_time_slots_reserved_within_capacity CHECK (reserved_count <= capacity),
    ADD CONSTRAINT chk_time_slots_time_order CHECK (start_time < end_time);
