-- Flyway Database Migration V2: MF-02 Operational Staff Identity Serialization Anchor Foundation
-- Canonical Section 29 & Business Rule MF02 Operational Staff Concurrency

CREATE TABLE IF NOT EXISTS operational_staff_identities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_operational_staff_identity_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backfill existing staff users from operational_staff_rosters if table exists
INSERT INTO operational_staff_identities (user_id, created_at)
SELECT DISTINCT user_id, CURRENT_TIMESTAMP
FROM operational_staff_rosters
WHERE user_id IS NOT NULL
ON DUPLICATE KEY UPDATE operational_staff_identities.user_id = operational_staff_identities.user_id;
