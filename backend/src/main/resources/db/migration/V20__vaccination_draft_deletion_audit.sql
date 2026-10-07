-- Flyway Database Migration V20: Vaccination Draft Deletion Audit and Correction Review
-- Database: MySQL 8.0+ / 8.4+ LTS

-- 1. Vaccination draft hard-deletion audit trail
CREATE TABLE vaccination_draft_deletion_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    vaccinator_id BIGINT NOT NULL,
    deleted_by BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    deleted_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccination_draft_deletion_draft
    ON vaccination_draft_deletion_audits(draft_id);

CREATE INDEX idx_vaccination_draft_deletion_appointment
    ON vaccination_draft_deletion_audits(appointment_id);

CREATE INDEX idx_vaccination_draft_deletion_time
    ON vaccination_draft_deletion_audits(deleted_at);

-- 2. Add review audit columns to vaccination_corrections
ALTER TABLE vaccination_corrections
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD COLUMN reviewed_at DATETIME(6) NULL,
    ADD COLUMN review_reason VARCHAR(1000) NULL;
