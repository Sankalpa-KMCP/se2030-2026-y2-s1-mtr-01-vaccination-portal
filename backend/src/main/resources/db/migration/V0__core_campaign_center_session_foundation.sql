-- Flyway Database Migration V0: Core Campaign, Center, Session, and Staff Roster Baseline Foundation
-- Pre-requisite baseline foundation ensuring V1-V9 historical chain integrity on fresh empty MySQL databases
-- Database: MySQL 8.0+ / 8.4+ LTS

-- 1. Vaccine Type Master Catalog
CREATE TABLE IF NOT EXISTS vaccine_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    dose_unit VARCHAR(32) NOT NULL DEFAULT 'dose',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccine_type_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Target Group Master Catalog
CREATE TABLE IF NOT EXISTS target_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_target_group_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Vaccination Campaign Core Entity
CREATE TABLE IF NOT EXISTS vaccination_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NULL,
    vaccine_type_id BIGINT NOT NULL,
    target_group_id BIGINT NOT NULL,
    service_start_date DATE NOT NULL,
    service_end_date DATE NOT NULL,
    booking_open_at DATETIME(6) NOT NULL,
    booking_close_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccination_campaign_code UNIQUE (code),
    CONSTRAINT fk_campaign_vaccine_type FOREIGN KEY (vaccine_type_id) REFERENCES vaccine_types (id) ON DELETE RESTRICT,
    CONSTRAINT fk_campaign_target_group FOREIGN KEY (target_group_id) REFERENCES target_groups (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_campaign_status ON vaccination_campaigns(status);
CREATE INDEX idx_campaign_service_window ON vaccination_campaigns(service_start_date, service_end_date);

-- 4. Vaccination Center Core Entity
CREATE TABLE IF NOT EXISTS vaccination_centers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    contact VARCHAR(128) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    center_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    safe_concurrent_occupancy INT NOT NULL,
    session_service_ceiling INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccination_center_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_center_status ON vaccination_centers(center_status);

-- 5. Vaccination Session Core Entity (pre-V9 state, without booking_suspended which is added by V9)
CREATE TABLE IF NOT EXISTS vaccination_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    recurrence_rule_id BIGINT NULL,
    occurrence_key VARCHAR(64) NULL,
    origin_occurrence_date DATE NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    booking_close_at DATETIME(6) NOT NULL,
    schedule_timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_session_occurrence_key UNIQUE (occurrence_key),
    CONSTRAINT fk_session_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE,
    CONSTRAINT fk_session_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_session_campaign_schedule ON vaccination_sessions(campaign_id, start_at, end_at, lifecycle_status);
CREATE INDEX idx_session_center_schedule ON vaccination_sessions(center_id, start_at, end_at, lifecycle_status);
CREATE INDEX idx_session_recurrence_origin ON vaccination_sessions(recurrence_rule_id, origin_occurrence_date);

-- 6. Operational Staff Roster Core Entity (pre-requisite for V2 backfill query)
CREATE TABLE IF NOT EXISTS operational_staff_rosters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_center_staff_user UNIQUE (center_id, user_id),
    CONSTRAINT fk_staff_roster_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_operational_staff_user ON operational_staff_rosters(user_id);
CREATE INDEX idx_operational_staff_center_role ON operational_staff_rosters(center_id, role, status);
