-- Flyway Database Migration V10: MF-02 Campaign and Session Supporting Schema
-- Completes remaining MF-02 operational tables for centers, campaigns, and sessions
-- Database: MySQL 8.0+ / 8.4+ LTS

-- 1. Campaign Location Targets
CREATE TABLE IF NOT EXISTS campaign_location_targets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_code VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_campaign_location_target UNIQUE (campaign_id, target_type, target_code),
    CONSTRAINT fk_clt_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Campaign Center Assignments
CREATE TABLE IF NOT EXISTS campaign_center_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    active_from DATE NOT NULL,
    active_to DATE NULL,
    assignment_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cca_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE,
    CONSTRAINT fk_cca_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_campaign_center_assignment_campaign_history ON campaign_center_assignments(campaign_id, active_from, id);
CREATE INDEX idx_campaign_center_assignment_center_history ON campaign_center_assignments(center_id, active_from, id);
CREATE INDEX idx_campaign_center_assignment_overlap ON campaign_center_assignments(campaign_id, center_id, assignment_status, active_from, active_to);

-- 3. Campaign Dose Programmes
CREATE TABLE IF NOT EXISTS campaign_dose_programmes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    dose_category VARCHAR(32) NOT NULL,
    permitted_dose_numbers VARCHAR(64) NOT NULL,
    minimum_interval_days INT NOT NULL DEFAULT 0,
    previous_evidence_required BOOLEAN NOT NULL DEFAULT FALSE,
    expected_service_minutes INT NOT NULL DEFAULT 10,
    active_from DATE NOT NULL,
    active_to DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_campaign_dose_programme_version UNIQUE (campaign_id, version_number),
    CONSTRAINT fk_cdp_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Campaign Lifecycle Events
CREATE TABLE IF NOT EXISTS campaign_lifecycle_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_cle_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_campaign_lifecycle_campaign_created ON campaign_lifecycle_events(campaign_id, created_at, id);

-- 5. Campaign Questionnaire Assignments
CREATE TABLE IF NOT EXISTS campaign_questionnaire_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    questionnaire_version_id BIGINT NOT NULL,
    active_from DATE NOT NULL,
    active_to DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cqa_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cqa_campaign_active ON campaign_questionnaire_assignments(campaign_id, active_from, active_to);
CREATE INDEX idx_cqa_qversion ON campaign_questionnaire_assignments(questionnaire_version_id);

-- 6. Center Operating Hours
CREATE TABLE IF NOT EXISTS operating_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    day_of_week VARCHAR(16) NOT NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_operating_hours_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_operating_hours_center_day ON operating_hours(center_id, day_of_week);

-- 7. Center Closures
CREATE TABLE IF NOT EXISTS center_closures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_center_closure_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_center_closure_center_start ON center_closures(center_id, start_at, id);
CREATE INDEX idx_center_closure_active_overlap ON center_closures(center_id, status, start_at, end_at);

-- 8. Center Operational Resources
CREATE TABLE IF NOT EXISTS operational_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_center_resource_code UNIQUE (center_id, code),
    CONSTRAINT fk_resource_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_operational_resource_center_type ON operational_resources(center_id, resource_type, status);

-- 9. Session Recurrence Rules
CREATE TABLE IF NOT EXISTS session_recurrence_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    recurrence_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    generation_key VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_recurrence_rule_generation_key UNIQUE (generation_key),
    CONSTRAINT fk_srr_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE CASCADE,
    CONSTRAINT fk_srr_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_recurrence_rule_campaign_range ON session_recurrence_rules(campaign_id, start_date, end_date, recurrence_status);
CREATE INDEX idx_recurrence_rule_center_range ON session_recurrence_rules(center_id, start_date, end_date, recurrence_status);

-- 10. Session Recurrence Rule Weekdays (ElementCollection)
CREATE TABLE IF NOT EXISTS session_recurrence_rule_weekdays (
    recurrence_rule_id BIGINT NOT NULL,
    day_of_week VARCHAR(16) NOT NULL,
    CONSTRAINT uk_recurrence_rule_weekday UNIQUE (recurrence_rule_id, day_of_week),
    CONSTRAINT fk_srrw_recurrence_rule FOREIGN KEY (recurrence_rule_id) REFERENCES session_recurrence_rules (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Connect FK from vaccination_sessions to session_recurrence_rules
ALTER TABLE vaccination_sessions
    ADD CONSTRAINT fk_session_recurrence_rule FOREIGN KEY (recurrence_rule_id) REFERENCES session_recurrence_rules (id) ON DELETE SET NULL;

-- 11. Session Capacity Plans
CREATE TABLE IF NOT EXISTS session_capacity_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    revision INT NOT NULL DEFAULT 1,
    duration_minutes INT NOT NULL,
    expected_service_minutes INT NOT NULL DEFAULT 10,
    assigned_vaccinator_count INT NOT NULL DEFAULT 0,
    available_station_count INT NOT NULL DEFAULT 0,
    fully_equipped_station_count INT NOT NULL DEFAULT 0,
    effective_station_count INT NOT NULL DEFAULT 0,
    recommended_throughput INT NOT NULL DEFAULT 0,
    approved_capacity INT NOT NULL DEFAULT 0,
    safe_concurrent_occupancy INT NOT NULL,
    expected_dwell_minutes INT NOT NULL DEFAULT 15,
    expected_accompanying_persons_per_booking DOUBLE NOT NULL DEFAULT 0.0,
    session_service_ceiling INT NULL,
    override_reason VARCHAR(255) NULL,
    approved_by VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_session_capacity_plan_revision UNIQUE (session_id, revision),
    CONSTRAINT fk_scp_session FOREIGN KEY (session_id) REFERENCES vaccination_sessions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_capacity_plan_session_rev ON session_capacity_plans(session_id, revision);

-- 12. Session Change Events
CREATE TABLE IF NOT EXISTS session_change_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    old_center_id BIGINT NOT NULL,
    old_schedule_timezone VARCHAR(64) NOT NULL,
    old_start_at DATETIME(6) NOT NULL,
    old_end_at DATETIME(6) NOT NULL,
    old_booking_close_at DATETIME(6) NOT NULL,
    old_lifecycle_status VARCHAR(16) NOT NULL,
    new_center_id BIGINT NOT NULL,
    new_schedule_timezone VARCHAR(64) NOT NULL,
    new_start_at DATETIME(6) NOT NULL,
    new_end_at DATETIME(6) NOT NULL,
    new_booking_close_at DATETIME(6) NOT NULL,
    new_lifecycle_status VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    affected_appointment_count INT NOT NULL DEFAULT 0,
    affected_waitlist_count INT NOT NULL DEFAULT 0,
    actor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_sce_session FOREIGN KEY (session_id) REFERENCES vaccination_sessions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_session_change_session_created ON session_change_events(session_id, created_at, id);

-- 13. Session Resource Assignments
CREATE TABLE IF NOT EXISTS session_resource_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sra_session FOREIGN KEY (session_id) REFERENCES vaccination_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_sra_resource FOREIGN KEY (resource_id) REFERENCES operational_resources (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resource_assignment_resource_window ON session_resource_assignments(resource_id, start_at, end_at);
CREATE INDEX idx_resource_assignment_session ON session_resource_assignments(session_id);

-- 14. Session Staff Assignments
CREATE TABLE IF NOT EXISTS session_staff_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    staff_roster_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ssa_session FOREIGN KEY (session_id) REFERENCES vaccination_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ssa_roster FOREIGN KEY (staff_roster_id) REFERENCES operational_staff_rosters (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_staff_assignment_roster_window ON session_staff_assignments(staff_roster_id, start_at, end_at);
CREATE INDEX idx_staff_assignment_user_window ON session_staff_assignments(user_id, start_at, end_at);
CREATE INDEX idx_staff_assignment_session ON session_staff_assignments(session_id);
