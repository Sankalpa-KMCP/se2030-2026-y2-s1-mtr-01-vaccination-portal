-- Shared authentication registration and staff-request lifecycle foundation.
-- Profile columns remain nullable so existing accounts continue to load unchanged.

ALTER TABLE user_accounts
    ADD COLUMN full_name VARCHAR(255) NULL AFTER id,
    ADD COLUMN phone_number VARCHAR(32) NULL AFTER email,
    ADD COLUMN requested_role_id BIGINT NULL AFTER account_status,
    ADD COLUMN reviewed_by_user_id BIGINT NULL AFTER requested_role_id,
    ADD COLUMN reviewed_at TIMESTAMP NULL AFTER reviewed_by_user_id,
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER reviewed_at,
    ADD CONSTRAINT fk_user_accounts_requested_role
        FOREIGN KEY (requested_role_id) REFERENCES roles(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_user_accounts_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts(id) ON DELETE SET NULL;

CREATE INDEX idx_user_accounts_status_created
    ON user_accounts(account_status, created_at);

CREATE INDEX idx_user_accounts_requested_role
    ON user_accounts(requested_role_id);
