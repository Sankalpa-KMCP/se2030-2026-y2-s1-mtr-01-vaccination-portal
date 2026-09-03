-- Flyway Database Migration V2: MF-03 Waitlist Entry Foundation
-- Canonical Section 7.4, 12.x & Business Rules MF03-BR-06 / MF03-BR-14

CREATE TABLE IF NOT EXISTS waitlist_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version BIGINT NOT NULL DEFAULT 0,
    active_campaign_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'WAITING' THEN campaign_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_waitlist_active_citizen_campaign UNIQUE (citizen_id, active_campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_waitlist_citizen ON waitlist_entry(citizen_id);
CREATE INDEX idx_waitlist_campaign ON waitlist_entry(campaign_id);
CREATE INDEX idx_waitlist_session_status_joined ON waitlist_entry(session_id, status, joined_at);
