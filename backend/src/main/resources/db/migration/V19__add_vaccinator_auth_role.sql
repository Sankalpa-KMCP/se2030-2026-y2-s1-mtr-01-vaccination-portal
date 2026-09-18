-- Flyway Database Migration V19: Add the global Vaccinator authentication role

INSERT INTO roles (code, name, created_at)
SELECT 'ROLE_VACCINATOR', 'Vaccinator', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'ROLE_VACCINATOR'
);
