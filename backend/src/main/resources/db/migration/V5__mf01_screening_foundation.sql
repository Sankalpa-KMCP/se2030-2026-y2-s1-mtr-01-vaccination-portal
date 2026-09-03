-- Flyway Database Migration V5: MF-01 Screening and Eligibility Foundation
-- Baseline schema aligned from database/migrations/V002__mf01_screening_foundation.sql
-- Database: MySQL 8.0+

-- 1. Consent and Privacy Notice Version
CREATE TABLE consent_notice_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_code VARCHAR(64) NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    retired_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_consent_notice_version UNIQUE (notice_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Screening Question Catalog
CREATE TABLE screening_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    text VARCHAR(1000) NOT NULL,
    description VARCHAR(1000) NULL,
    question_type VARCHAR(32) NOT NULL,
    default_required BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Answer Option for Selectable Questions
CREATE TABLE answer_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    typed_value VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_answer_option_question FOREIGN KEY (question_id) REFERENCES screening_question (id) ON DELETE RESTRICT,
    CONSTRAINT uq_answer_option_question_code UNIQUE (question_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Logical Questionnaire Container
CREATE TABLE questionnaire (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Questionnaire Version (Immutable once published)
CREATE TABLE questionnaire_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    questionnaire_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    retired_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_questionnaire_version_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire (id) ON DELETE RESTRICT,
    CONSTRAINT uq_questionnaire_version UNIQUE (questionnaire_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Questionnaire Item (Ordered question inclusion in version)
CREATE TABLE questionnaire_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    required_override BOOLEAN NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_questionnaire_item_version FOREIGN KEY (version_id) REFERENCES questionnaire_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_questionnaire_item_question FOREIGN KEY (question_id) REFERENCES screening_question (id) ON DELETE RESTRICT,
    CONSTRAINT uq_questionnaire_item_order UNIQUE (version_id, display_order),
    CONSTRAINT uq_questionnaire_item_question UNIQUE (version_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Eligibility Rule (Snapshot to a questionnaire version)
CREATE TABLE eligibility_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    version_id BIGINT NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    condition_expression TEXT NOT NULL,
    preliminary_result VARCHAR(64) NOT NULL DEFAULT 'PRELIMINARY_REVIEW_REQUIRED',
    priority INT NOT NULL DEFAULT 100,
    message VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_eligibility_rule_version FOREIGN KEY (version_id) REFERENCES questionnaire_version (id) ON DELETE CASCADE,
    CONSTRAINT uq_eligibility_rule_code UNIQUE (version_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Screening Submission (Citizen submission per campaign)
CREATE TABLE screening_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    consent_notice_version_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    preliminary_result VARCHAR(64) NULL,
    consent_at DATETIME(6) NOT NULL,
    submitted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_screening_sub_version FOREIGN KEY (version_id) REFERENCES questionnaire_version (id) ON DELETE RESTRICT,
    CONSTRAINT fk_screening_sub_consent FOREIGN KEY (consent_notice_version_id) REFERENCES consent_notice_version (id) ON DELETE RESTRICT,
    INDEX idx_sub_citizen_campaign (citizen_id, campaign_id),
    INDEX idx_sub_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Screening Answer (Responses to individual questions)
CREATE TABLE screening_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    typed_answer TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_screening_ans_submission FOREIGN KEY (submission_id) REFERENCES screening_submission (id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_ans_question FOREIGN KEY (question_id) REFERENCES screening_question (id) ON DELETE RESTRICT,
    CONSTRAINT uq_screening_ans_sub_quest UNIQUE (submission_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Eligibility Assessment (Healthcare Officer final review decision)
CREATE TABLE eligibility_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL UNIQUE,
    citizen_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    preliminary_result VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    clinical_notes TEXT NULL,
    reviewed_at DATETIME(6) NOT NULL,
    valid_until DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    revocation_reason TEXT NULL,
    rescreening_authorized_at DATETIME(6) NULL,
    rescreening_authorized_by BIGINT NULL,
    rescreening_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_eligibility_assessment_sub FOREIGN KEY (submission_id) REFERENCES screening_submission (id) ON DELETE RESTRICT,
    INDEX idx_assess_citizen_campaign (citizen_id, campaign_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
