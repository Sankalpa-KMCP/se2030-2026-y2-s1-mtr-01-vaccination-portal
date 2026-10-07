-- Flyway Database Migration V21: MF-01 Screening Submission Active-Uniqueness Invariant (R05)
-- Enforces, at the database level, the application rule "at most ONE active screening submission
-- per citizen per campaign" (ScreeningSubmissionService: DRAFT / PENDING_REVIEW / CORRECTIONS_REQUIRED
-- are the active statuses; REVIEWED is terminal and does not block legitimate later rescreening).
--
-- Technique: MySQL generated column + unique constraint, identical to the established pattern used
-- by the appointment (uk_appointment_active_citizen_campaign, V1) and waitlist_entry
-- (uk_waitlist_active_citizen_campaign, V7) tables. The generated column evaluates to campaign_id
-- while the row is active and to NULL otherwise, so only active rows participate in the unique key.
--
-- Existing data: rows that already violate the invariant (two or more active submissions for the
-- same citizen/campaign) will make this migration fail with a duplicate-entry error so the data can
-- be reconciled deliberately before upgrading; a clean schema is unaffected.

ALTER TABLE screening_submission
    ADD COLUMN active_campaign_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('DRAFT', 'PENDING_REVIEW', 'CORRECTIONS_REQUIRED') THEN campaign_id ELSE NULL END
    ) STORED,
    ADD CONSTRAINT uk_screening_submission_active_citizen_campaign UNIQUE (citizen_id, active_campaign_id);
