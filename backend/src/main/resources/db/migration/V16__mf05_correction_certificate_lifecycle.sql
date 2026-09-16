-- Flyway Database Migration V15: MF-05 Correction and Certificate Lifecycle
-- Evolves the V11 foundation without rewriting existing vaccination history.
-- Database: MySQL 8.0.16+ / 8.4+ LTS

-- 1. Append-only vaccination correction history
CREATE TABLE vaccination_corrections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vaccination_record_id BIGINT NOT NULL,
    field_name VARCHAR(40) NOT NULL,
    old_value VARCHAR(2000) NULL,
    new_value VARCHAR(2000) NULL,
    reason VARCHAR(1000) NOT NULL,
    corrected_by BIGINT NOT NULL,
    corrected_at DATETIME(6) NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT chk_vaccination_correction_field CHECK (
        field_name IN (
            'VACCINE_BATCH_ID',
            'DOSE_NUMBER',
            'ADMINISTRATION_TIME',
            'NEXT_DOSE_DATE',
            'AUTHORIZED_NOTES'
        )
    ),
    CONSTRAINT chk_vaccination_correction_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'APPLIED')
    ),
    CONSTRAINT fk_vaccination_correction_record
        FOREIGN KEY (vaccination_record_id)
        REFERENCES vaccination_records (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccination_correction_record_time
    ON vaccination_corrections(vaccination_record_id, corrected_at, id);

-- 2. Certificate issue snapshots and lifecycle audit columns are nullable for V11 data.
ALTER TABLE digital_certificates
    ADD COLUMN issued_vaccine_batch_id BIGINT NULL,
    ADD COLUMN issued_dose_number INT NULL,
    ADD COLUMN issued_administration_time DATETIME(6) NULL,
    ADD COLUMN revoked_at DATETIME(6) NULL,
    ADD COLUMN revoked_by BIGINT NULL,
    ADD COLUMN revocation_reason VARCHAR(1000) NULL,
    ADD COLUMN reissued_from_certificate_id BIGINT NULL,
    ADD COLUMN superseded_by_certificate_id BIGINT NULL,
    ADD COLUMN superseded_at DATETIME(6) NULL,
    ADD COLUMN superseded_by BIGINT NULL,
    ADD COLUMN supersession_reason VARCHAR(1000) NULL;

-- 3. Preserve existing certificates by capturing their immutable finalized-record values.
UPDATE digital_certificates certificate
JOIN vaccination_records record
  ON record.id = certificate.vaccination_record_id
SET certificate.issued_vaccine_batch_id = record.vaccine_batch_id,
    certificate.issued_dose_number = record.dose_number,
    certificate.issued_administration_time = record.administration_time
WHERE certificate.issued_vaccine_batch_id IS NULL
   OR certificate.issued_dose_number IS NULL
   OR certificate.issued_administration_time IS NULL;

-- 4. Align persisted certificate lifecycle values with CertificateStatus.
ALTER TABLE digital_certificates
    ADD CONSTRAINT chk_digital_certificate_status
        CHECK (status IN ('VALID', 'REVOKED', 'SUPERSEDED'));

-- 5. MySQL-compatible conditional uniqueness: historical rows produce NULL markers.
-- Remove the V11 cascading foreign key before adding a generated column that
-- depends on its base column; the final restrictive foreign key is restored below.
ALTER TABLE digital_certificates
    DROP FOREIGN KEY fk_digital_certificate_record;

ALTER TABLE digital_certificates
    ADD COLUMN valid_vaccination_record_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'VALID' THEN vaccination_record_id
                ELSE NULL
            END
        ) STORED,
    ADD CONSTRAINT uk_digital_certificate_valid_record
        UNIQUE (valid_vaccination_record_id);

-- 6. Deterministic history and lifecycle-link indexes.
CREATE INDEX idx_digital_certificate_record_issued
    ON digital_certificates(vaccination_record_id, issued_at, id);

CREATE INDEX idx_digital_certificate_reissued_from
    ON digital_certificates(reissued_from_certificate_id);

CREATE INDEX idx_digital_certificate_superseded_by
    ON digital_certificates(superseded_by_certificate_id);

-- 7. Lifecycle links preserve history and never cascade-delete certificate rows.
ALTER TABLE digital_certificates
    ADD CONSTRAINT fk_digital_certificate_reissued_from
        FOREIGN KEY (reissued_from_certificate_id)
        REFERENCES digital_certificates (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_digital_certificate_superseded_by
        FOREIGN KEY (superseded_by_certificate_id)
        REFERENCES digital_certificates (id)
        ON DELETE RESTRICT;

-- 8. Replace the V11 one-certificate-ever rule with historical lifecycle support.
ALTER TABLE digital_certificates
    DROP INDEX uk_digital_certificate_record;

ALTER TABLE digital_certificates
    ADD CONSTRAINT fk_digital_certificate_record
        FOREIGN KEY (vaccination_record_id)
        REFERENCES vaccination_records (id)
        ON DELETE RESTRICT;
