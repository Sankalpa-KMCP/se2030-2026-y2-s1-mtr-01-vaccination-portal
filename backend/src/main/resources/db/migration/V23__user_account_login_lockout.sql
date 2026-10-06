-- Flyway Database Migration V23: Durable per-account login brute-force lockout counter
-- Canonical Appendix E (Security/manual lock): system-initiated timed lock persists the
-- evidence in user_accounts.locked_until; this migration adds the durable failed-login
-- attempt counter that drives the automatic threshold transition.
-- Additive and safe for existing rows: every account starts with zero recorded failures.

ALTER TABLE user_accounts
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
