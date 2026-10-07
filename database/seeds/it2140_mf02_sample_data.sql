-- ============================================================================
-- IT2140 Database Design and Development Assignment Part 02 - Sample Data Seed
-- Module: MF-02 — Vaccination Campaign, Center and Session Management
-- Script: database/seeds/it2140_mf02_sample_data.sql
-- Database: MySQL 8.4 LTS (vaccination_portal)
-- Invariant: Ensures every MF-02 table contains AT LEAST 5 valid records.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- STEP 1: Vaccine Types (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO vaccine_types (code, name, dose_unit, active, created_at, updated_at)
SELECT 'HEPB', 'Hepatitis B Recombinant Vaccine', 'dose', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccine_types WHERE code = 'HEPB');

-- ----------------------------------------------------------------------------
-- STEP 2: Target Groups (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO target_groups (code, description, active, created_at, updated_at)
SELECT 'PREGNANT-WOMEN', 'Pregnant Women and Expectant Mothers', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM target_groups WHERE code = 'PREGNANT-WOMEN');

-- ----------------------------------------------------------------------------
-- STEP 3: Vaccination Centers (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO vaccination_centers (
    code, name, address, contact, timezone, center_status,
    safe_concurrent_occupancy, session_service_ceiling, created_at, updated_at
)
SELECT
    'CTR-COLOMBO-01',
    'National Hospital Vaccination Center Colombo',
    'No 12 Regent Street, Colombo 10, Western Province, Sri Lanka',
    '+94112691111',
    'Asia/Colombo',
    'ACTIVE',
    120,
    500,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01');

-- ----------------------------------------------------------------------------
-- STEP 4: Vaccination Campaigns (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO vaccination_campaigns (
    code, name, description, vaccine_type_id, target_group_id,
    service_start_date, service_end_date, booking_open_at, booking_close_at,
    status, created_at, updated_at
)
SELECT
    'CMP-INFLUENZA-2026',
    'National Influenza Protection Campaign 2026',
    'Annual seasonal influenza immunization campaign targeting senior citizens and vulnerable cohorts.',
    (SELECT id FROM vaccine_types WHERE code = 'INFLUENZA-QUAD'),
    (SELECT id FROM target_groups WHERE code = 'ELDERLY-65-PLUS'),
    '2026-10-01',
    '2026-12-31',
    '2026-09-15 00:00:00.000000',
    '2026-12-30 23:59:59.000000',
    'ACTIVE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026');

INSERT INTO vaccination_campaigns (
    code, name, description, vaccine_type_id, target_group_id,
    service_start_date, service_end_date, booking_open_at, booking_close_at,
    status, created_at, updated_at
)
SELECT
    'CMP-HEPB-2026',
    'Healthcare Personnel Hepatitis B Immunisation Campaign 2026',
    'Compulsory three-dose recombinant Hepatitis B immunization program for healthcare workforce.',
    (SELECT id FROM vaccine_types WHERE code = 'HEPB'),
    (SELECT id FROM target_groups WHERE code = 'HEALTHCARE-WORKERS'),
    '2026-10-15',
    '2027-04-30',
    '2026-10-01 00:00:00.000000',
    '2027-04-29 23:59:59.000000',
    'SCHEDULED',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026');

-- ----------------------------------------------------------------------------
-- STEP 5: Supporting Test Staff Accounts (Supporting non-MF02 data)
-- ----------------------------------------------------------------------------
INSERT INTO user_accounts (full_name, email, password_hash, account_status, email_verified_at, created_at, updated_at)
SELECT 'Dr. Kamal Perera', 'mf02.staff1@example.test', '$2a$10$wK1W2G6cQ/W4qC1l5D5URe0I5eKzF3n0k4h9l1v7t2m8s4o1q3a5u', 'ACTIVE', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_accounts WHERE email = 'mf02.staff1@example.test');

INSERT INTO user_accounts (full_name, email, password_hash, account_status, email_verified_at, created_at, updated_at)
SELECT 'Sister Dilani Silva', 'mf02.staff2@example.test', '$2a$10$wK1W2G6cQ/W4qC1l5D5URe0I5eKzF3n0k4h9l1v7t2m8s4o1q3a5u', 'ACTIVE', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_accounts WHERE email = 'mf02.staff2@example.test');

INSERT INTO user_accounts (full_name, email, password_hash, account_status, email_verified_at, created_at, updated_at)
SELECT 'Officer Nuwan Fernando', 'mf02.staff3@example.test', '$2a$10$wK1W2G6cQ/W4qC1l5D5URe0I5eKzF3n0k4h9l1v7t2m8s4o1q3a5u', 'ACTIVE', NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_accounts WHERE email = 'mf02.staff3@example.test');

-- Map Role Authorizations
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_accounts u, roles r
WHERE u.email = 'mf02.staff1@example.test' AND r.code = 'ROLE_VACCINATOR';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_accounts u, roles r
WHERE u.email = 'mf02.staff2@example.test' AND r.code = 'ROLE_VACCINATOR';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_accounts u, roles r
WHERE u.email = 'mf02.staff3@example.test' AND r.code = 'ROLE_HEALTHCARE_OFFICER';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_accounts u, roles r
WHERE u.email = 'staff@example.com' AND r.code = 'ROLE_VACCINATOR';

-- Initialize Concurrency Anchor Identities
INSERT IGNORE INTO operational_staff_identities (user_id, created_at)
SELECT id, NOW()
FROM user_accounts
WHERE email IN (
    'staff@example.com',
    'hco@local.test',
    'mf02.staff1@example.test',
    'mf02.staff2@example.test',
    'mf02.staff3@example.test'
);

-- ----------------------------------------------------------------------------
-- STEP 6: Operational Staff Rosters (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    (SELECT id FROM user_accounts WHERE email = 'mf02.staff1@example.test'),
    'VACCINATOR', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM operational_staff_rosters
    WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01')
      AND user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff1@example.test')
);

INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    (SELECT id FROM user_accounts WHERE email = 'mf02.staff2@example.test'),
    'VACCINATOR', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM operational_staff_rosters
    WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01')
      AND user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff2@example.test')
);

INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    (SELECT id FROM user_accounts WHERE email = 'staff@example.com'),
    'SUPERVISOR', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM operational_staff_rosters
    WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01')
      AND user_id = (SELECT id FROM user_accounts WHERE email = 'staff@example.com')
);

INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    (SELECT id FROM user_accounts WHERE email = 'hco@local.test'),
    'CHECK_IN_STAFF', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM operational_staff_rosters
    WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01')
      AND user_id = (SELECT id FROM user_accounts WHERE email = 'hco@local.test')
);

INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    (SELECT id FROM user_accounts WHERE email = 'mf02.staff3@example.test'),
    'SUPERVISOR', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM operational_staff_rosters
    WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01')
      AND user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff3@example.test')
);

-- ----------------------------------------------------------------------------
-- STEP 7: Campaign Location Targets (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO campaign_location_targets (campaign_id, target_type, target_code, created_at, updated_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'), 'PROVINCE', 'CP', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_location_targets
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026')
      AND target_type = 'PROVINCE' AND target_code = 'CP'
);

INSERT INTO campaign_location_targets (campaign_id, target_type, target_code, created_at, updated_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'), 'PROVINCE', 'WP', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_location_targets
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026')
      AND target_type = 'PROVINCE' AND target_code = 'WP'
);

INSERT INTO campaign_location_targets (campaign_id, target_type, target_code, created_at, updated_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'), 'DISTRICT', 'KANDY', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_location_targets
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026')
      AND target_type = 'DISTRICT' AND target_code = 'KANDY'
);

INSERT INTO campaign_location_targets (campaign_id, target_type, target_code, created_at, updated_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026'), 'DISTRICT', 'COLOMBO', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_location_targets
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026')
      AND target_type = 'DISTRICT' AND target_code = 'COLOMBO'
);

INSERT INTO campaign_location_targets (campaign_id, target_type, target_code, created_at, updated_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026'), 'HEALTH_REGION', 'HR-WEST', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_location_targets
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026')
      AND target_type = 'HEALTH_REGION' AND target_code = 'HR-WEST'
);

-- ----------------------------------------------------------------------------
-- STEP 8: Campaign Center Assignments (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO campaign_center_assignments (campaign_id, center_id, active_from, active_to, assignment_status, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    '2026-10-01',
    '2026-12-31',
    'ACTIVE',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_center_assignments
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026')
      AND center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01')
);

-- ----------------------------------------------------------------------------
-- STEP 9: Campaign Dose Programmes (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO campaign_dose_programmes (
    campaign_id, version_number, dose_category, permitted_dose_numbers,
    minimum_interval_days, previous_evidence_required, expected_service_minutes,
    active_from, active_to, created_at, updated_at
)
SELECT 1, 1, 'PRIMARY', '1,2', 21, FALSE, 10, '2026-09-01', NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM campaign_dose_programmes WHERE campaign_id = 1 AND version_number = 1);

INSERT INTO campaign_dose_programmes (
    campaign_id, version_number, dose_category, permitted_dose_numbers,
    minimum_interval_days, previous_evidence_required, expected_service_minutes,
    active_from, active_to, created_at, updated_at
)
SELECT 1, 2, 'BOOSTER', '3', 90, TRUE, 10, '2026-09-15', NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM campaign_dose_programmes WHERE campaign_id = 1 AND version_number = 2);

INSERT INTO campaign_dose_programmes (
    campaign_id, version_number, dose_category, permitted_dose_numbers,
    minimum_interval_days, previous_evidence_required, expected_service_minutes,
    active_from, active_to, created_at, updated_at
)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'), 1, 'PRIMARY', '1', 0, FALSE, 10, '2026-10-01', NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_dose_programmes
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026') AND version_number = 1
);

INSERT INTO campaign_dose_programmes (
    campaign_id, version_number, dose_category, permitted_dose_numbers,
    minimum_interval_days, previous_evidence_required, expected_service_minutes,
    active_from, active_to, created_at, updated_at
)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026'), 1, 'PRIMARY', '1,2,3', 28, FALSE, 15, '2026-10-15', NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM campaign_dose_programmes
    WHERE campaign_id = (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026') AND version_number = 1
);

INSERT INTO campaign_dose_programmes (
    campaign_id, version_number, dose_category, permitted_dose_numbers,
    minimum_interval_days, previous_evidence_required, expected_service_minutes,
    active_from, active_to, created_at, updated_at
)
SELECT 2, 1, 'PRIMARY', '1', 0, FALSE, 10, '2026-09-01', NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM campaign_dose_programmes WHERE campaign_id = 2 AND version_number = 1);

-- ----------------------------------------------------------------------------
-- STEP 10: Campaign Lifecycle Events (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO campaign_lifecycle_events (campaign_id, change_type, previous_status, new_status, reason, actor_id, created_at)
SELECT 1, 'SUSPENDED', 'ACTIVE', 'SUSPENDED', 'Temporary supply cold-chain audit suspension', 'admin@local.test', '2026-09-05 10:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM campaign_lifecycle_events WHERE campaign_id = 1 AND change_type = 'SUSPENDED' AND reason = 'Temporary supply cold-chain audit suspension');

INSERT INTO campaign_lifecycle_events (campaign_id, change_type, previous_status, new_status, reason, actor_id, created_at)
SELECT 1, 'RESUMED', 'SUSPENDED', 'ACTIVE', 'Cold-chain audit successfully cleared; operations resumed', 'admin@local.test', '2026-09-06 08:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM campaign_lifecycle_events WHERE campaign_id = 1 AND change_type = 'RESUMED' AND reason = 'Cold-chain audit successfully cleared; operations resumed');

INSERT INTO campaign_lifecycle_events (campaign_id, change_type, previous_status, new_status, reason, actor_id, created_at)
SELECT (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'), 'COMPLETED', 'ACTIVE', 'COMPLETED', 'Pilot vaccination phase reached full demographic target', 'admin@local.test', '2026-09-20 18:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM campaign_lifecycle_events WHERE reason = 'Pilot vaccination phase reached full demographic target');

INSERT INTO campaign_lifecycle_events (campaign_id, change_type, previous_status, new_status, reason, actor_id, created_at)
SELECT 3, 'CANCELLED', 'DRAFT', 'CANCELLED', 'Draft campaign replaced by canonical national schedule', 'admin@local.test', '2026-09-18 12:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM campaign_lifecycle_events WHERE campaign_id = 3 AND change_type = 'CANCELLED' AND reason = 'Draft campaign replaced by canonical national schedule');

-- ----------------------------------------------------------------------------
-- STEP 11: Center Closures (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO center_closures (center_id, start_at, end_at, reason, status, created_at, updated_at)
SELECT (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), '2026-10-15 00:00:00.000000', '2026-10-15 23:59:59.000000', 'Public Holiday - Facility Sanitation Day', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM center_closures WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01') AND start_at = '2026-10-15 00:00:00.000000');

INSERT INTO center_closures (center_id, start_at, end_at, reason, status, created_at, updated_at)
SELECT (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), '2026-11-01 06:00:00.000000', '2026-11-01 12:00:00.000000', 'Substation Electrical Infrastructure Maintenance', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM center_closures WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01') AND start_at = '2026-11-01 06:00:00.000000');

INSERT INTO center_closures (center_id, start_at, end_at, reason, status, created_at, updated_at)
SELECT (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), '2026-10-20 08:00:00.000000', '2026-10-20 18:00:00.000000', 'Cold Chain Freezer Backup Upgrades', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM center_closures WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01') AND start_at = '2026-10-20 08:00:00.000000');

INSERT INTO center_closures (center_id, start_at, end_at, reason, status, created_at, updated_at)
SELECT (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), '2026-11-12 00:00:00.000000', '2026-11-12 23:59:59.000000', 'National Observance Facility Closure', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM center_closures WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01') AND start_at = '2026-11-12 00:00:00.000000');

INSERT INTO center_closures (center_id, start_at, end_at, reason, status, created_at, updated_at)
SELECT (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), '2026-09-10 08:00:00.000000', '2026-09-10 14:00:00.000000', 'Safety Evacuation and Emergency Response Drill', 'CANCELLED', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM center_closures WHERE center_id = (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01') AND start_at = '2026-09-10 08:00:00.000000');

-- ----------------------------------------------------------------------------
-- STEP 12: Operational Resources (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO operational_resources (center_id, code, resource_type, name, status, created_at, updated_at)
VALUES
((SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'RES-KDY-STN-01', 'VACCINATION_STATION', 'Vaccination Station Alpha', 'ACTIVE', NOW(6), NOW(6)),
((SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'RES-KDY-STN-02', 'VACCINATION_STATION', 'Vaccination Station Beta', 'ACTIVE', NOW(6), NOW(6)),
((SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'RES-KDY-RM-01', 'ROOM', 'Post-Observation Hall 1', 'ACTIVE', NOW(6), NOW(6)),
((SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), 'RES-CMB-STN-01', 'VACCINATION_STATION', 'Main Hall Station 1', 'ACTIVE', NOW(6), NOW(6)),
((SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), 'RES-CMB-EQ-01', 'EQUIPMENT', 'Mobile Ultra-Low Cold Storage Unit', 'ACTIVE', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

-- ----------------------------------------------------------------------------
-- STEP 13 & 14: Session Recurrence Rules & Weekdays (Minimum target: 5 each)
-- ----------------------------------------------------------------------------
INSERT INTO session_recurrence_rules (
    campaign_id, center_id, start_date, end_date, start_time, end_time,
    timezone, recurrence_status, generation_key, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    '2026-10-01', '2026-10-31', '08:30:00', '12:30:00',
    'Asia/Colombo', 'ACTIVE', 'a1000000-0000-0000-0000-000000000001', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000001');

INSERT INTO session_recurrence_rules (
    campaign_id, center_id, start_date, end_date, start_time, end_time,
    timezone, recurrence_status, generation_key, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    '2026-10-01', '2026-10-31', '09:00:00', '13:00:00',
    'Asia/Colombo', 'ACTIVE', 'a1000000-0000-0000-0000-000000000002', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000002');

INSERT INTO session_recurrence_rules (
    campaign_id, center_id, start_date, end_date, start_time, end_time,
    timezone, recurrence_status, generation_key, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    '2026-10-15', '2026-11-15', '13:00:00', '17:00:00',
    'Asia/Colombo', 'ACTIVE', 'a1000000-0000-0000-0000-000000000003', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000003');

INSERT INTO session_recurrence_rules (
    campaign_id, center_id, start_date, end_date, start_time, end_time,
    timezone, recurrence_status, generation_key, created_at, updated_at
)
SELECT
    1,
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    '2026-10-01', '2026-10-25', '08:00:00', '12:00:00',
    'Asia/Colombo', 'ACTIVE', 'a1000000-0000-0000-0000-000000000004', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000004');

INSERT INTO session_recurrence_rules (
    campaign_id, center_id, start_date, end_date, start_time, end_time,
    timezone, recurrence_status, generation_key, created_at, updated_at
)
SELECT
    1,
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    '2026-10-05', '2026-10-30', '10:00:00', '14:00:00',
    'Asia/Colombo', 'ACTIVE', 'a1000000-0000-0000-0000-000000000005', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000005');

-- Recurrence Rule Weekdays Mapping
INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'MONDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000001';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'WEDNESDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000001';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'TUESDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000002';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'THURSDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000002';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'FRIDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000003';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'SATURDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000004';

INSERT IGNORE INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week)
SELECT id, 'MONDAY' FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000005';

-- ----------------------------------------------------------------------------
-- STEP 15: Vaccination Sessions (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO vaccination_sessions (
    campaign_id, center_id, recurrence_rule_id, occurrence_key, origin_occurrence_date,
    start_at, end_at, booking_close_at, schedule_timezone, lifecycle_status,
    booking_suspended, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    (SELECT id FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000001'),
    'OCC-INFL-KDY-20261005',
    '2026-10-05',
    '2026-10-05 08:30:00.000000',
    '2026-10-05 12:30:00.000000',
    '2026-10-04 18:00:00.000000',
    'Asia/Colombo',
    'SCHEDULED',
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005');

INSERT INTO vaccination_sessions (
    campaign_id, center_id, recurrence_rule_id, occurrence_key, origin_occurrence_date,
    start_at, end_at, booking_close_at, schedule_timezone, lifecycle_status,
    booking_suspended, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-INFLUENZA-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    (SELECT id FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000002'),
    'OCC-INFL-CMB-20261006',
    '2026-10-06',
    '2026-10-06 09:00:00.000000',
    '2026-10-06 13:00:00.000000',
    '2026-10-05 18:00:00.000000',
    'Asia/Colombo',
    'SCHEDULED',
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006');

INSERT INTO vaccination_sessions (
    campaign_id, center_id, recurrence_rule_id, occurrence_key, origin_occurrence_date,
    start_at, end_at, booking_close_at, schedule_timezone, lifecycle_status,
    booking_suspended, created_at, updated_at
)
SELECT
    (SELECT id FROM vaccination_campaigns WHERE code = 'CMP-HEPB-2026'),
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'),
    (SELECT id FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000003'),
    'OCC-HEPB-CMB-20261016',
    '2026-10-16',
    '2026-10-16 13:00:00.000000',
    '2026-10-16 17:00:00.000000',
    '2026-10-15 18:00:00.000000',
    'Asia/Colombo',
    'SCHEDULED',
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_sessions WHERE occurrence_key = 'OCC-HEPB-CMB-20261016');

INSERT INTO vaccination_sessions (
    campaign_id, center_id, recurrence_rule_id, occurrence_key, origin_occurrence_date,
    start_at, end_at, booking_close_at, schedule_timezone, lifecycle_status,
    booking_suspended, created_at, updated_at
)
SELECT
    1,
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'),
    (SELECT id FROM session_recurrence_rules WHERE generation_key = 'a1000000-0000-0000-0000-000000000004'),
    'OCC-COV-KDY-20261010',
    '2026-10-10',
    '2026-10-10 08:00:00.000000',
    '2026-10-10 12:00:00.000000',
    '2026-10-09 18:00:00.000000',
    'Asia/Colombo',
    'IN_PROGRESS',
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM vaccination_sessions WHERE occurrence_key = 'OCC-COV-KDY-20261010');

-- ----------------------------------------------------------------------------
-- STEP 16: Time Slots with Capacity Invariants (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-05 08:30:00'),
    TIMESTAMP('2026-10-05 09:30:00'),
    20, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-KDY-20261005'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-05 08:30:00'));

INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-05 09:30:00'),
    TIMESTAMP('2026-10-05 10:30:00'),
    20, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-KDY-20261005'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-05 09:30:00'));

INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-05 10:30:00'),
    TIMESTAMP('2026-10-05 11:30:00'),
    20, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-KDY-20261005'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-05 10:30:00'));

INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-06 09:00:00'),
    TIMESTAMP('2026-10-06 10:00:00'),
    25, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-CMB-20261006'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-06 09:00:00'));

INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-06 10:00:00'),
    TIMESTAMP('2026-10-06 11:00:00'),
    25, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-CMB-20261006'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-06 10:00:00'));

INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at)
SELECT
    s.id,
    TIMESTAMP('2026-10-16 13:00:00'),
    TIMESTAMP('2026-10-16 14:00:00'),
    15, 0, 0, NOW(), NOW()
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-HEPB-CMB-20261016'
  AND NOT EXISTS (SELECT 1 FROM time_slots WHERE session_id = s.id AND start_time = TIMESTAMP('2026-10-16 13:00:00'));

-- ----------------------------------------------------------------------------
-- STEP 17: Session Capacity Plans (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO session_capacity_plans (
    session_id, revision, duration_minutes, expected_service_minutes,
    assigned_vaccinator_count, available_station_count, fully_equipped_station_count,
    effective_station_count, recommended_throughput, approved_capacity,
    safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking,
    session_service_ceiling, override_reason, approved_by, created_at, updated_at
)
SELECT
    1, 1, 420, 10,
    2, 2, 2, 2,
    12, 60, 60, 15, 0.0, 100, NULL, 'admin@local.test', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM session_capacity_plans WHERE session_id = 1 AND revision = 1);

INSERT INTO session_capacity_plans (
    session_id, revision, duration_minutes, expected_service_minutes,
    assigned_vaccinator_count, available_station_count, fully_equipped_station_count,
    effective_station_count, recommended_throughput, approved_capacity,
    safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking,
    session_service_ceiling, override_reason, approved_by, created_at, updated_at
)
SELECT
    s.id, 1, 240, 10,
    2, 2, 2, 2,
    12, 60, 80, 15, 0.0, 120, NULL, 'admin@local.test', NOW(6), NOW(6)
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-KDY-20261005'
  AND NOT EXISTS (SELECT 1 FROM session_capacity_plans WHERE session_id = s.id AND revision = 1);

INSERT INTO session_capacity_plans (
    session_id, revision, duration_minutes, expected_service_minutes,
    assigned_vaccinator_count, available_station_count, fully_equipped_station_count,
    effective_station_count, recommended_throughput, approved_capacity,
    safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking,
    session_service_ceiling, override_reason, approved_by, created_at, updated_at
)
SELECT
    s.id, 1, 240, 10,
    2, 2, 2, 2,
    12, 50, 120, 15, 0.0, 150, NULL, 'admin@local.test', NOW(6), NOW(6)
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-INFL-CMB-20261006'
  AND NOT EXISTS (SELECT 1 FROM session_capacity_plans WHERE session_id = s.id AND revision = 1);

INSERT INTO session_capacity_plans (
    session_id, revision, duration_minutes, expected_service_minutes,
    assigned_vaccinator_count, available_station_count, fully_equipped_station_count,
    effective_station_count, recommended_throughput, approved_capacity,
    safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking,
    session_service_ceiling, override_reason, approved_by, created_at, updated_at
)
SELECT
    s.id, 1, 240, 15,
    2, 2, 2, 2,
    8, 30, 120, 15, 0.0, 100, NULL, 'admin@local.test', NOW(6), NOW(6)
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-HEPB-CMB-20261016'
  AND NOT EXISTS (SELECT 1 FROM session_capacity_plans WHERE session_id = s.id AND revision = 1);

INSERT INTO session_capacity_plans (
    session_id, revision, duration_minutes, expected_service_minutes,
    assigned_vaccinator_count, available_station_count, fully_equipped_station_count,
    effective_station_count, recommended_throughput, approved_capacity,
    safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking,
    session_service_ceiling, override_reason, approved_by, created_at, updated_at
)
SELECT
    s.id, 1, 240, 10,
    2, 2, 2, 2,
    12, 40, 80, 15, 0.0, 100, NULL, 'admin@local.test', NOW(6), NOW(6)
FROM vaccination_sessions s
WHERE s.occurrence_key = 'OCC-COV-KDY-20261010'
  AND NOT EXISTS (SELECT 1 FROM session_capacity_plans WHERE session_id = s.id AND revision = 1);

-- ----------------------------------------------------------------------------
-- STEP 18: Session Change Events (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO session_change_events (
    session_id, change_type, old_center_id, old_schedule_timezone,
    old_start_at, old_end_at, old_booking_close_at, old_lifecycle_status,
    new_center_id, new_schedule_timezone, new_start_at, new_end_at,
    new_booking_close_at, new_lifecycle_status, reason,
    affected_appointment_count, affected_waitlist_count, actor_id, created_at
)
SELECT
    1, 'RESCHEDULED', 2, 'UTC',
    '2026-09-18 02:00:00.000000', '2026-09-18 09:00:00.000000', '2026-09-17 18:00:00.000000', 'DRAFT',
    2, 'UTC',
    '2026-09-18 02:42:00.000000', '2026-09-18 09:42:00.000000', '2026-09-17 18:00:00.000000', 'DRAFT',
    'Shift start time aligned with clinical preparation protocol', 0, 0, 'admin@local.test', '2026-09-17 14:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM session_change_events WHERE session_id = 1 AND change_type = 'RESCHEDULED' AND created_at = '2026-09-17 14:00:00.000000');

INSERT INTO session_change_events (
    session_id, change_type, old_center_id, old_schedule_timezone,
    old_start_at, old_end_at, old_booking_close_at, old_lifecycle_status,
    new_center_id, new_schedule_timezone, new_start_at, new_end_at,
    new_booking_close_at, new_lifecycle_status, reason,
    affected_appointment_count, affected_waitlist_count, actor_id, created_at
)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    'BOOKING_SUSPENDED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', '2026-10-04 18:00:00.000000', 'SCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', '2026-10-04 18:00:00.000000', 'SCHEDULED',
    'Cold chain temperature audit temporary hold', 0, 0, 'admin@local.test', '2026-09-20 09:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM session_change_events WHERE reason = 'Cold chain temperature audit temporary hold');

INSERT INTO session_change_events (
    session_id, change_type, old_center_id, old_schedule_timezone,
    old_start_at, old_end_at, old_booking_close_at, old_lifecycle_status,
    new_center_id, new_schedule_timezone, new_start_at, new_end_at,
    new_booking_close_at, new_lifecycle_status, reason,
    affected_appointment_count, affected_waitlist_count, actor_id, created_at
)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    'BOOKING_RESUMED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', '2026-10-04 18:00:00.000000', 'SCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', '2026-10-04 18:00:00.000000', 'SCHEDULED',
    'Cold chain verification confirmed safe; bookings resumed', 0, 0, 'admin@local.test', '2026-09-20 11:30:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM session_change_events WHERE reason = 'Cold chain verification confirmed safe; bookings resumed');

INSERT INTO session_change_events (
    session_id, change_type, old_center_id, old_schedule_timezone,
    old_start_at, old_end_at, old_booking_close_at, old_lifecycle_status,
    new_center_id, new_schedule_timezone, new_start_at, new_end_at,
    new_booking_close_at, new_lifecycle_status, reason,
    affected_appointment_count, affected_waitlist_count, actor_id, created_at
)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006'),
    'RESCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), 'Asia/Colombo',
    '2026-10-06 09:00:00.000000', '2026-10-06 12:00:00.000000', '2026-10-05 18:00:00.000000', 'SCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-COLOMBO-01'), 'Asia/Colombo',
    '2026-10-06 09:00:00.000000', '2026-10-06 13:00:00.000000', '2026-10-05 18:00:00.000000', 'SCHEDULED',
    'Extended operational window by 60 minutes for high demand', 0, 0, 'admin@local.test', '2026-09-22 10:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM session_change_events WHERE reason = 'Extended operational window by 60 minutes for high demand');

INSERT INTO session_change_events (
    session_id, change_type, old_center_id, old_schedule_timezone,
    old_start_at, old_end_at, old_booking_close_at, old_lifecycle_status,
    new_center_id, new_schedule_timezone, new_start_at, new_end_at,
    new_booking_close_at, new_lifecycle_status, reason,
    affected_appointment_count, affected_waitlist_count, actor_id, created_at
)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-COV-KDY-20261010'),
    'RESCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-10 08:00:00.000000', '2026-10-10 12:00:00.000000', '2026-10-09 18:00:00.000000', 'SCHEDULED',
    (SELECT id FROM vaccination_centers WHERE code = 'CTR-KANDY-01'), 'Asia/Colombo',
    '2026-10-10 08:00:00.000000', '2026-10-10 12:00:00.000000', '2026-10-09 18:00:00.000000', 'IN_PROGRESS',
    'Session transition to IN_PROGRESS upon station doors opening', 0, 0, 'admin@local.test', '2026-09-25 08:00:00.000000'
WHERE NOT EXISTS (SELECT 1 FROM session_change_events WHERE reason = 'Session transition to IN_PROGRESS upon station doors opening');

-- ----------------------------------------------------------------------------
-- STEP 19: Session Resource Assignments (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO session_resource_assignments (session_id, resource_id, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    (SELECT id FROM operational_resources WHERE code = 'RES-KDY-STN-01'),
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM session_resource_assignments
    WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
      AND resource_id = (SELECT id FROM operational_resources WHERE code = 'RES-KDY-STN-01')
);

INSERT INTO session_resource_assignments (session_id, resource_id, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    (SELECT id FROM operational_resources WHERE code = 'RES-KDY-STN-02'),
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM session_resource_assignments
    WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
      AND resource_id = (SELECT id FROM operational_resources WHERE code = 'RES-KDY-STN-02')
);

INSERT INTO session_resource_assignments (session_id, resource_id, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    (SELECT id FROM operational_resources WHERE code = 'RES-KDY-RM-01'),
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM session_resource_assignments
    WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
      AND resource_id = (SELECT id FROM operational_resources WHERE code = 'RES-KDY-RM-01')
);

INSERT INTO session_resource_assignments (session_id, resource_id, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006'),
    (SELECT id FROM operational_resources WHERE code = 'RES-CMB-STN-01'),
    '2026-10-06 09:00:00.000000', '2026-10-06 13:00:00.000000', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM session_resource_assignments
    WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006')
      AND resource_id = (SELECT id FROM operational_resources WHERE code = 'RES-CMB-STN-01')
);

INSERT INTO session_resource_assignments (session_id, resource_id, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-HEPB-CMB-20261016'),
    (SELECT id FROM operational_resources WHERE code = 'RES-CMB-EQ-01'),
    '2026-10-16 13:00:00.000000', '2026-10-16 17:00:00.000000', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM session_resource_assignments
    WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-HEPB-CMB-20261016')
      AND resource_id = (SELECT id FROM operational_resources WHERE code = 'RES-CMB-EQ-01')
);

-- ----------------------------------------------------------------------------
-- STEP 20: Session Staff Assignments (Minimum target: 5)
-- ----------------------------------------------------------------------------
INSERT INTO session_staff_assignments (session_id, staff_roster_id, user_id, role, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    r.id,
    r.user_id,
    'VACCINATOR',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
FROM operational_staff_rosters r
WHERE r.user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff1@example.test')
  AND NOT EXISTS (
      SELECT 1 FROM session_staff_assignments
      WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
        AND staff_roster_id = r.id
  );

INSERT INTO session_staff_assignments (session_id, staff_roster_id, user_id, role, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    r.id,
    r.user_id,
    'VACCINATOR',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
FROM operational_staff_rosters r
WHERE r.user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff2@example.test')
  AND NOT EXISTS (
      SELECT 1 FROM session_staff_assignments
      WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
        AND staff_roster_id = r.id
  );

INSERT INTO session_staff_assignments (session_id, staff_roster_id, user_id, role, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005'),
    r.id,
    r.user_id,
    'SUPERVISOR',
    '2026-10-05 08:30:00.000000', '2026-10-05 12:30:00.000000', NOW(6), NOW(6)
FROM operational_staff_rosters r
WHERE r.user_id = (SELECT id FROM user_accounts WHERE email = 'staff@example.com')
  AND NOT EXISTS (
      SELECT 1 FROM session_staff_assignments
      WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-KDY-20261005')
        AND staff_roster_id = r.id
  );

INSERT INTO session_staff_assignments (session_id, staff_roster_id, user_id, role, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006'),
    r.id,
    r.user_id,
    'CHECK_IN_STAFF',
    '2026-10-06 09:00:00.000000', '2026-10-06 13:00:00.000000', NOW(6), NOW(6)
FROM operational_staff_rosters r
WHERE r.user_id = (SELECT id FROM user_accounts WHERE email = 'hco@local.test')
  AND NOT EXISTS (
      SELECT 1 FROM session_staff_assignments
      WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006')
        AND staff_roster_id = r.id
  );

INSERT INTO session_staff_assignments (session_id, staff_roster_id, user_id, role, start_at, end_at, created_at, updated_at)
SELECT
    (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006'),
    r.id,
    r.user_id,
    'SUPERVISOR',
    '2026-10-06 09:00:00.000000', '2026-10-06 13:00:00.000000', NOW(6), NOW(6)
FROM operational_staff_rosters r
WHERE r.user_id = (SELECT id FROM user_accounts WHERE email = 'mf02.staff3@example.test')
  AND NOT EXISTS (
      SELECT 1 FROM session_staff_assignments
      WHERE session_id = (SELECT id FROM vaccination_sessions WHERE occurrence_key = 'OCC-INFL-CMB-20261006')
        AND staff_roster_id = r.id
  );
