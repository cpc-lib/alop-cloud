CREATE TABLE customer_vehicle (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    plate_no_ciphertext VARCHAR(512) NOT NULL,
    plate_no_hash VARCHAR(128) NOT NULL,
    vehicle_type VARCHAR(32) NOT NULL,
    brand VARCHAR(64) NULL,
    model VARCHAR(64) NULL,
    color VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    KEY idx_vehicle_customer (tenant_id, customer_id, status),
    KEY idx_vehicle_plate_hash (tenant_id, plate_no_hash)
);
