-- Flyway Database Migration V4: Shared Authentication and Identity Foundation
-- Canonical Section 12.2 (UserAccount, Role, UserRole, RefreshTokenSession)

CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMP NULL,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_token_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_family_id VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    rotated_from_id BIGINT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    reuse_detected_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Static Canonical Reference Roles (No user credential seeds)
INSERT INTO roles (code, name, created_at) VALUES
    ('ROLE_ADMIN', 'System Administrator', CURRENT_TIMESTAMP),
    ('ROLE_CENTER_MANAGER', 'Vaccination Center Manager', CURRENT_TIMESTAMP),
    ('ROLE_HEALTHCARE_OFFICER', 'Healthcare Officer', CURRENT_TIMESTAMP),
    ('ROLE_CAMPAIGN_OFFICER', 'Campaign or Health Authority Officer', CURRENT_TIMESTAMP),
    ('ROLE_INVENTORY_OFFICER', 'Inventory Officer', CURRENT_TIMESTAMP),
    ('ROLE_CITIZEN', 'Citizen', CURRENT_TIMESTAMP),
    ('ROLE_MANAGEMENT_USER', 'Management or Reporting User', CURRENT_TIMESTAMP);
