-- Flyway Database Migration V24: Index for refresh token family revocation and lookup
-- Canonical Section 17.1 / Finding F-04: Support rapid lookup and revocation of active
-- refresh token sessions by token_family_id and revoked_at during token rotation and reuse detection.

CREATE INDEX idx_refresh_token_sessions_family_revoked
    ON refresh_token_sessions (token_family_id, revoked_at);
