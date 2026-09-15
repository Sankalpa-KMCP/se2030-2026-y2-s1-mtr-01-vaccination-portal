-- Flyway Database Migration V6: MF-01 Screening Correction Metadata

ALTER TABLE screening_submission
    ADD COLUMN correction_instructions TEXT NULL,
    ADD COLUMN correction_requested_at DATETIME(6) NULL;
