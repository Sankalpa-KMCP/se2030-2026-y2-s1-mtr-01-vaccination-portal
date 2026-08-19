-- Flyway Database Migration V1: MF-03 Appointment and Citizen Campaign Booking Guard Foundation
-- Canonical Section 12.2, 12.4 & Business Rule MF03-BR-02

CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    active_campaign_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('CONFIRMED', 'CHECKED_IN') THEN campaign_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_appointment_active_citizen_campaign UNIQUE (citizen_id, active_campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointment_citizen ON appointment(citizen_id);
CREATE INDEX idx_appointment_campaign ON appointment(campaign_id);
CREATE INDEX idx_appointment_slot ON appointment(slot_id);
CREATE INDEX idx_appointment_status ON appointment(status);

CREATE TABLE IF NOT EXISTS citizen_campaign_booking_guard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_booking_guard_citizen_campaign UNIQUE (citizen_id, campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_booking_guard_citizen ON citizen_campaign_booking_guard(citizen_id);
CREATE INDEX idx_booking_guard_campaign ON citizen_campaign_booking_guard(campaign_id);
