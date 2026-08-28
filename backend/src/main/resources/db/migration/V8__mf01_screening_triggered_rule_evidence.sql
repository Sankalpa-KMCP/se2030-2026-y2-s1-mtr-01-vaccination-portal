-- Flyway Database Migration V8: MF-01 Triggered Rule Evidence Snapshot

CREATE TABLE screening_triggered_rule_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    source_rule_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    message VARCHAR(500) NOT NULL,
    preliminary_result VARCHAR(64) NOT NULL,
    priority INT NOT NULL,
    evaluation_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_triggered_rule_evidence_submission
        FOREIGN KEY (submission_id) REFERENCES screening_submission (id) ON DELETE CASCADE,
    CONSTRAINT fk_triggered_rule_evidence_rule
        FOREIGN KEY (source_rule_id) REFERENCES eligibility_rule (id) ON DELETE RESTRICT,
    CONSTRAINT uq_triggered_rule_evidence_submission_rule UNIQUE (submission_id, source_rule_id),
    INDEX idx_triggered_rule_evidence_submission_order (submission_id, evaluation_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
