-- Flyway Database Migration V22: Citizen Email OTP Verification Challenge
-- Canonical Section 12.2 / Section 17.1 extension (email OTP verification).
-- The email_verified_at column on user_accounts already exists since V4; this migration
-- adds the Auth-owned one-current-challenge-per-account OTP persistence model and treats
-- every account that is already usable (ACTIVE) at migration time as email verified so
-- existing users are never locked out by the upcoming appointment booking enforcement.

CREATE TABLE IF NOT EXISTS email_otp_challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    target_email VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    consumed_at TIMESTAMP NULL,
    invalidated_at TIMESTAMP NULL,
    last_sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_otp_challenges_user UNIQUE (user_id),
    CONSTRAINT fk_email_otp_challenges_user FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email_otp_challenges_expiry
    ON email_otp_challenges(user_id, expires_at);

-- Existing accounts present at migration time remain fully usable: every ACTIVE account
-- is treated as already email verified. Accounts still PENDING_VERIFICATION keep their
-- existing link-based verification path and are NOT auto-verified.
UPDATE user_accounts
SET email_verified_at = CURRENT_TIMESTAMP
WHERE email_verified_at IS NULL
  AND account_status = 'ACTIVE';
