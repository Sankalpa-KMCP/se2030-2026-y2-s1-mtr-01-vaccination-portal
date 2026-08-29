-- Flyway Database Migration V11: MF-05 Vaccination Administration and Digital Certificate Foundation
-- Establishes vaccination draft, finalized record, and certificate tables
-- Database: MySQL 8.0+ / 8.4+ LTS

-- 1. Vaccination Drafts
CREATE TABLE IF NOT EXISTS vaccination_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    vaccinator_id BIGINT NULL,
    vaccine_batch_id BIGINT NULL,
    dose_number INT NULL,
    administration_time DATETIME(6) NULL,
    next_dose_date DATE NULL,
    authorized_notes VARCHAR(2000) NULL,
    physical_administration_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccination_draft_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_vaccination_draft_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccination_draft_status ON vaccination_drafts(status);
CREATE INDEX idx_vaccination_draft_batch ON vaccination_drafts(vaccine_batch_id);

-- 2. Vaccination Finalized Records
CREATE TABLE IF NOT EXISTS vaccination_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    vaccinator_id BIGINT NOT NULL,
    vaccine_batch_id BIGINT NOT NULL,
    dose_number INT NOT NULL,
    administration_time DATETIME(6) NOT NULL,
    next_dose_date DATE NULL,
    authorized_notes VARCHAR(2000) NULL,
    finalized_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccination_record_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_vaccination_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id) ON DELETE RESTRICT,
    CONSTRAINT fk_vaccination_record_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns (id) ON DELETE RESTRICT,
    CONSTRAINT fk_vaccination_record_center FOREIGN KEY (center_id) REFERENCES vaccination_centers (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccination_record_citizen_administered ON vaccination_records(citizen_id, administration_time);
CREATE INDEX idx_vaccination_record_batch ON vaccination_records(vaccine_batch_id);

-- 3. Digital Certificates
CREATE TABLE IF NOT EXISTS digital_certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vaccination_record_id BIGINT NOT NULL,
    certificate_reference VARCHAR(64) NOT NULL,
    issued_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(16) NOT NULL DEFAULT 'VALID',
    verification_token VARCHAR(128) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_digital_certificate_record UNIQUE (vaccination_record_id),
    CONSTRAINT uk_digital_certificate_reference UNIQUE (certificate_reference),
    CONSTRAINT uk_digital_certificate_verification_token UNIQUE (verification_token),
    CONSTRAINT fk_digital_certificate_record FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
