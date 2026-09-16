-- Flyway Database Migration V16: Canonical Super Administrator Role Seed
-- Canonical Section 12.2 (Role/UserRole)
-- Seeds canonical ROLE_SUPER_ADMIN into the roles table.
-- Does not seed any default user accounts or credentials.

INSERT INTO roles (code, name, created_at)
SELECT 'ROLE_SUPER_ADMIN', 'Super Administrator', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'ROLE_SUPER_ADMIN'
);
