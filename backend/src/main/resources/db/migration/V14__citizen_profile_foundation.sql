-- Citizen self-service profile foundation.
-- Existing citizens remain valid without a profile row; future registrations create one.

CREATE TABLE citizen_profiles (
    user_id BIGINT NOT NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(32) NULL,
    address VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_citizen_profiles_user
        FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
