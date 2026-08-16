CREATE TABLE refund_amount_reservation (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    reservation_no VARCHAR(64) NOT NULL,
    refund_request_id VARCHAR(128) NOT NULL,
    payment_order_id BIGINT NOT NULL,
    original_payment_transaction_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    requested_amount DECIMAL(18,2) NOT NULL,
    reserved_amount DECIMAL(18,2) NOT NULL,
    confirmed_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    expire_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_reservation_no (tenant_id, reservation_no),
    UNIQUE KEY uk_refund_request (tenant_id, refund_request_id),
    KEY idx_refund_payment (tenant_id, payment_order_id, status),
    KEY idx_refund_reserved_status (tenant_id, status, created_at)
);

CREATE TABLE refund_reservation_target (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    refund_reservation_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    reserved_amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_target (tenant_id, refund_reservation_id, target_type, target_id),
    KEY idx_refund_target_source (tenant_id, target_type, target_id)
);
