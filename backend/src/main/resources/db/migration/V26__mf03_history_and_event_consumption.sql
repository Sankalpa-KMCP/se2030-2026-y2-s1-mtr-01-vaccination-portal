CREATE TABLE appointment_change_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    source_session_change_event_id BIGINT NULL,
    change_type VARCHAR(40) NOT NULL,
    old_slot_id BIGINT NULL,
    new_slot_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    actor_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_change_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointment(id),
    CONSTRAINT fk_appointment_change_session_event
        FOREIGN KEY (source_session_change_event_id) REFERENCES session_change_events(id),
    CONSTRAINT uk_appointment_change_session_event
        UNIQUE (appointment_id, source_session_change_event_id, change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointment_change_history
    ON appointment_change_event(appointment_id, created_at, id);

CREATE TABLE waitlist_change_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    waitlist_entry_id BIGINT NOT NULL,
    source_session_change_event_id BIGINT NULL,
    old_status VARCHAR(40) NULL,
    new_status VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NULL,
    actor_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_waitlist_change_entry
        FOREIGN KEY (waitlist_entry_id) REFERENCES waitlist_entry(id),
    CONSTRAINT fk_waitlist_change_session_event
        FOREIGN KEY (source_session_change_event_id) REFERENCES session_change_events(id),
    CONSTRAINT uk_waitlist_change_session_event
        UNIQUE (waitlist_entry_id, source_session_change_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_waitlist_change_history
    ON waitlist_change_event(waitlist_entry_id, created_at, id);

CREATE TABLE check_in_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    event_type VARCHAR(24) NOT NULL,
    actor_id VARCHAR(64) NULL,
    event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500) NULL,
    CONSTRAINT fk_check_in_event_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_check_in_event_history
    ON check_in_event(appointment_id, event_at, id);

CREATE TABLE module_event_consumption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consumer_code VARCHAR(64) NOT NULL,
    source_event_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_module_event_consumer_source UNIQUE (consumer_code, source_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_module_event_consumption_status
    ON module_event_consumption(consumer_code, status, source_event_id);

CREATE TABLE appointment_notification_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    citizen_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    CONSTRAINT fk_appointment_notification_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointment_notification_pending
    ON appointment_notification_outbox(status, created_at, id);

-- Add the ownership and aggregate links omitted by the original MF-03 foundation.
ALTER TABLE appointment
    ADD CONSTRAINT fk_appointment_citizen FOREIGN KEY (citizen_id) REFERENCES user_accounts(id),
    ADD CONSTRAINT fk_appointment_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns(id),
    ADD CONSTRAINT fk_appointment_slot FOREIGN KEY (slot_id) REFERENCES time_slots(id);

ALTER TABLE waitlist_entry
    ADD CONSTRAINT fk_waitlist_citizen FOREIGN KEY (citizen_id) REFERENCES user_accounts(id),
    ADD CONSTRAINT fk_waitlist_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns(id),
    ADD CONSTRAINT fk_waitlist_session FOREIGN KEY (session_id) REFERENCES vaccination_sessions(id);

ALTER TABLE citizen_campaign_booking_guard
    ADD CONSTRAINT fk_booking_guard_citizen FOREIGN KEY (citizen_id) REFERENCES user_accounts(id),
    ADD CONSTRAINT fk_booking_guard_campaign FOREIGN KEY (campaign_id) REFERENCES vaccination_campaigns(id);
