-- Flyway Database Migration V26: Add request_type to vaccination_corrections
-- Distinguishes CITIZEN_CERTIFICATE_ISSUE and VACCINATOR_RECORD_AMENDMENT
-- Database: MySQL 8.0+ / 8.4+ LTS

ALTER TABLE vaccination_corrections
    ADD COLUMN request_type VARCHAR(32) NOT NULL DEFAULT 'CITIZEN_CERTIFICATE_ISSUE';

ALTER TABLE vaccination_corrections
    ADD CONSTRAINT chk_vaccination_correction_type
        CHECK (request_type IN ('CITIZEN_CERTIFICATE_ISSUE', 'VACCINATOR_RECORD_AMENDMENT'));

CREATE INDEX idx_vaccination_correction_type
    ON vaccination_corrections(request_type, status);
