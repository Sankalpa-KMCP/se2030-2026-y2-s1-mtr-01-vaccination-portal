-- Flyway Database Migration V15: MF-04 Vaccine Inventory and Batch Management Foundation
-- Canonical Sections 8, 12.2, 12.3 and Business Rules MF04-BR-01 through MF04-BR-12

CREATE TABLE manufacturers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_manufacturer_code UNIQUE (code),
    CONSTRAINT uk_manufacturer_name UNIQUE (name),
    CONSTRAINT chk_manufacturer_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_manufacturer_name_not_blank CHECK (TRIM(name) <> '')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vaccine_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manufacturer_id BIGINT NOT NULL,
    vaccine_type_id BIGINT NOT NULL,
    batch_number VARCHAR(128) NOT NULL,
    manufacture_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_vaccine_batch_identity
        UNIQUE (manufacturer_id, vaccine_type_id, batch_number),
    CONSTRAINT fk_vaccine_batch_manufacturer
        FOREIGN KEY (manufacturer_id) REFERENCES manufacturers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vaccine_batch_vaccine_type
        FOREIGN KEY (vaccine_type_id) REFERENCES vaccine_types(id) ON DELETE RESTRICT,
    CONSTRAINT chk_vaccine_batch_number_not_blank CHECK (TRIM(batch_number) <> ''),
    CONSTRAINT chk_vaccine_batch_dates CHECK (manufacture_date < expiry_date),
    CONSTRAINT chk_vaccine_batch_status CHECK (
        batch_status IN ('ACTIVE', 'QUARANTINED', 'RECALLED', 'EXPIRED', 'INACTIVE')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vaccine_batch_vaccine_type ON vaccine_batches(vaccine_type_id);
CREATE INDEX idx_vaccine_batch_status ON vaccine_batches(batch_status);
CREATE INDEX idx_vaccine_batch_expiry_date ON vaccine_batches(expiry_date);

CREATE TABLE center_inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_center_inventory_center_batch UNIQUE (center_id, batch_id),
    CONSTRAINT fk_center_inventory_center
        FOREIGN KEY (center_id) REFERENCES vaccination_centers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_center_inventory_batch
        FOREIGN KEY (batch_id) REFERENCES vaccine_batches(id) ON DELETE RESTRICT,
    CONSTRAINT chk_center_inventory_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_center_inventory_batch ON center_inventories(batch_id);
CREATE INDEX idx_center_inventory_center_quantity ON center_inventories(center_id, quantity);

CREATE TABLE stock_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id VARCHAR(64) NOT NULL,
    batch_id BIGINT NOT NULL,
    source_center_id BIGINT NOT NULL,
    destination_center_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by BIGINT NOT NULL,
    completed_at DATETIME(6) NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_stock_transfer_correlation UNIQUE (correlation_id),
    CONSTRAINT fk_stock_transfer_batch
        FOREIGN KEY (batch_id) REFERENCES vaccine_batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_transfer_source_center
        FOREIGN KEY (source_center_id) REFERENCES vaccination_centers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_transfer_destination_center
        FOREIGN KEY (destination_center_id) REFERENCES vaccination_centers(id) ON DELETE RESTRICT,
    CONSTRAINT chk_stock_transfer_correlation_not_blank CHECK (TRIM(correlation_id) <> ''),
    CONSTRAINT chk_stock_transfer_quantity CHECK (quantity > 0),
    CONSTRAINT chk_stock_transfer_distinct_centers CHECK (source_center_id <> destination_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_stock_transfer_batch ON stock_transfers(batch_id);
CREATE INDEX idx_stock_transfer_source_center ON stock_transfers(source_center_id);
CREATE INDEX idx_stock_transfer_destination_center ON stock_transfers(destination_center_id);
CREATE INDEX idx_stock_transfer_requested_by ON stock_transfers(requested_by);
CREATE INDEX idx_stock_transfer_status_created ON stock_transfers(status, created_at);

CREATE TABLE stock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    stock_transfer_id BIGINT NULL,
    type VARCHAR(32) NOT NULL,
    delta INT NOT NULL,
    before_qty INT NOT NULL,
    after_qty INT NOT NULL,
    actor_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    vaccination_record_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_stock_transaction_vaccination_record UNIQUE (vaccination_record_id),
    CONSTRAINT uk_stock_transaction_transfer_type UNIQUE (stock_transfer_id, type),
    CONSTRAINT fk_stock_transaction_inventory
        FOREIGN KEY (inventory_id) REFERENCES center_inventories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_transaction_transfer
        FOREIGN KEY (stock_transfer_id) REFERENCES stock_transfers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_transaction_vaccination_record
        FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records(id) ON DELETE RESTRICT,
    CONSTRAINT chk_stock_transaction_type CHECK (
        type IN (
            'RECEIPT',
            'TRANSFER_OUT',
            'TRANSFER_IN',
            'ADJUSTMENT_INCREASE',
            'ADJUSTMENT_DECREASE',
            'VACCINATION_DEDUCTION'
        )
    ),
    CONSTRAINT chk_stock_transaction_delta_nonzero CHECK (delta <> 0),
    CONSTRAINT chk_stock_transaction_quantities_nonnegative CHECK (
        before_qty >= 0 AND after_qty >= 0
    ),
    CONSTRAINT chk_stock_transaction_quantity_balance CHECK (
        after_qty = before_qty + delta
    ),
    CONSTRAINT chk_stock_transaction_delta_direction CHECK (
        (type IN ('RECEIPT', 'TRANSFER_IN', 'ADJUSTMENT_INCREASE') AND delta > 0)
        OR
        (type IN ('TRANSFER_OUT', 'ADJUSTMENT_DECREASE', 'VACCINATION_DEDUCTION') AND delta < 0)
    ),
    CONSTRAINT chk_stock_transaction_vaccination_dose CHECK (
        type <> 'VACCINATION_DEDUCTION' OR delta = -1
    ),
    CONSTRAINT chk_stock_transaction_actor CHECK (
        type = 'VACCINATION_DEDUCTION' OR actor_id IS NOT NULL
    ),
    CONSTRAINT chk_stock_transaction_transfer_link CHECK (
        (type IN ('TRANSFER_OUT', 'TRANSFER_IN') AND stock_transfer_id IS NOT NULL)
        OR
        (type NOT IN ('TRANSFER_OUT', 'TRANSFER_IN') AND stock_transfer_id IS NULL)
    ),
    CONSTRAINT chk_stock_transaction_vaccination_link CHECK (
        (type = 'VACCINATION_DEDUCTION' AND vaccination_record_id IS NOT NULL)
        OR
        (type <> 'VACCINATION_DEDUCTION' AND vaccination_record_id IS NULL)
    ),
    CONSTRAINT chk_stock_transaction_adjustment_reason CHECK (
        type NOT IN ('ADJUSTMENT_INCREASE', 'ADJUSTMENT_DECREASE')
        OR (reason IS NOT NULL AND TRIM(reason) <> '')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_stock_transaction_inventory_created
    ON stock_transactions(inventory_id, created_at);
CREATE INDEX idx_stock_transaction_transfer ON stock_transactions(stock_transfer_id);
CREATE INDEX idx_stock_transaction_actor_created ON stock_transactions(actor_id, created_at);
CREATE INDEX idx_stock_transaction_type_created ON stock_transactions(type, created_at);
