-- MF03-BR-06: active waitlist uniqueness is scoped to a session.
ALTER TABLE waitlist_entry
    DROP INDEX uk_waitlist_active_citizen_campaign;

ALTER TABLE waitlist_entry
    DROP COLUMN active_campaign_id;

ALTER TABLE waitlist_entry
    ADD COLUMN active_session_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'WAITING' THEN session_id ELSE NULL END
    ) STORED,
    ADD CONSTRAINT uk_waitlist_active_citizen_session
        UNIQUE (citizen_id, active_session_id);
