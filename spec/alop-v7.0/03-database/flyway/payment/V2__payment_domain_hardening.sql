-- V6.3 Payment domain hardening.
-- This migration extends V1 without deleting historical facts.

ALTER TABLE payment_order
    ADD COLUMN payment_scene VARCHAR(32) NULL AFTER customer_id,
    ADD COLUMN subject VARCHAR(256) NULL AFTER amount,
    ADD COLUMN description VARCHAR(512) NULL AFTER subject,
    ADD COLUMN intent_hash CHAR(64) NULL AFTER description,
    ADD COLUMN active_attempt_id BIGINT NULL AFTER intent_hash,
    ADD COLUMN paid_transaction_id BIGINT NULL AFTER active_attempt_id,
    ADD COLUMN paid_at DATETIME(3) NULL AFTER expire_at,
    ADD COLUMN closed_at DATETIME(3) NULL AFTER paid_at,
    ADD COLUMN last_status_source VARCHAR(32) NULL AFTER closed_at,
    ADD COLUMN status_reason_code VARCHAR(64) NULL AFTER last_status_source,
    ADD COLUMN created_by BIGINT NULL AFTER version,
    ADD KEY idx_payment_intent (tenant_id, customer_id, intent_hash, status, expire_at),
    ADD KEY idx_payment_status_expire (tenant_id, status, expire_at);

ALTER TABLE payment_business_relation
    ADD COLUMN business_no_snapshot VARCHAR(128) NULL AFTER business_id,
    ADD COLUMN charge_type_snapshot VARCHAR(32) NULL AFTER expected_amount,
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ADD UNIQUE KEY uk_payment_biz_relation (tenant_id, payment_order_id, business_type, business_id);

CREATE TABLE tenant_payment_merchant (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    merchant_mode VARCHAR(32) NOT NULL,
    merchant_id VARCHAR(128) NOT NULL,
    app_id VARCHAR(128) NULL,
    credential_ref VARCHAR(512) NOT NULL,
    certificate_ref VARCHAR(512) NULL,
    callback_profile VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    effective_from DATETIME(3) NOT NULL,
    effective_to DATETIME(3) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_merchant (tenant_id, channel, merchant_id, app_id),
    KEY idx_merchant_resolve (channel, merchant_id, app_id, status),
    KEY idx_tenant_channel_merchant (tenant_id, channel, status, priority)
);

CREATE TABLE payment_attempt (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    attempt_no VARCHAR(64) NOT NULL,
    payment_order_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    merchant_config_id BIGINT NOT NULL,
    provider_request_no VARCHAR(128) NOT NULL,
    provider_prepay_id VARCHAR(256) NULL,
    provider_status VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    client_action_payload JSON NULL,
    expire_at DATETIME(3) NULL,
    unknown_since DATETIME(3) NULL,
    last_query_at DATETIME(3) NULL,
    query_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempt_no (attempt_no),
    UNIQUE KEY uk_provider_request (channel, merchant_config_id, provider_request_no),
    KEY idx_attempt_order (tenant_id, payment_order_id, status),
    KEY idx_attempt_unknown (tenant_id, status, unknown_since, id)
);

CREATE TABLE payment_channel_request (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    request_no VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    merchant_config_id BIGINT NOT NULL,
    provider_request_id VARCHAR(128) NULL,
    request_hash VARCHAR(128) NULL,
    response_hash VARCHAR(128) NULL,
    http_status INT NULL,
    result_type VARCHAR(32) NOT NULL,
    provider_code VARCHAR(128) NULL,
    provider_message VARCHAR(512) NULL,
    duration_ms BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_request_no (request_no),
    KEY idx_channel_request_biz (tenant_id, business_type, business_id, created_at),
    KEY idx_channel_request_provider (channel, merchant_config_id, provider_request_id)
);

ALTER TABLE payment_transaction
    ADD COLUMN attempt_id BIGINT NULL AFTER payment_order_id,
    ADD COLUMN provider_merchant_id VARCHAR(128) NULL AFTER channel,
    ADD COLUMN provider_app_id VARCHAR(128) NULL AFTER provider_merchant_id,
    ADD COLUMN provider_status VARCHAR(64) NULL AFTER channel_trade_no,
    ADD COLUMN channel_fee_amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN settlement_amount DECIMAL(18,2) NULL AFTER channel_fee_amount,
    ADD COLUMN payer_reference_hash VARCHAR(128) NULL AFTER currency,
    ADD COLUMN success_at DATETIME(3) NULL AFTER occurred_at,
    ADD COLUMN notify_at DATETIME(3) NULL AFTER success_at,
    ADD COLUMN last_status_source VARCHAR(32) NULL AFTER notify_at,
    DROP INDEX uk_channel_trade,
    ADD UNIQUE KEY uk_channel_trade (channel, provider_merchant_id, channel_trade_no),
    ADD KEY idx_pt_attempt (tenant_id, attempt_id);

ALTER TABLE payment_callback_log
    ADD COLUMN app_ref VARCHAR(128) NULL AFTER merchant_ref,
    ADD COLUMN callback_type VARCHAR(32) NOT NULL DEFAULT 'PAYMENT' AFTER channel,
    ADD COLUMN provider_trade_no VARCHAR(128) NULL AFTER payment_no,
    ADD COLUMN payload_file_id BIGINT NULL AFTER body_hash,
    ADD COLUMN verified_tenant_id BIGINT NULL AFTER signature_valid,
    ADD COLUMN error_code VARCHAR(128) NULL AFTER process_result,
    ADD COLUMN processed_at DATETIME(3) NULL AFTER received_at,
    ADD KEY idx_cb_trade (channel, merchant_ref, provider_trade_no, received_at),
    ADD KEY idx_cb_result (process_result, received_at);

CREATE TABLE payment_order_state_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    payment_order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    status_source VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NULL,
    evidence_type VARCHAR(32) NULL,
    evidence_id VARCHAR(128) NULL,
    trace_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_payment_state_order (tenant_id, payment_order_id, created_at)
);

ALTER TABLE refund_order
    ADD COLUMN refund_reason_code VARCHAR(64) NULL AFTER refund_amount,
    ADD COLUMN refund_reservation_id VARCHAR(128) NULL AFTER reason,
    ADD COLUMN idempotency_key VARCHAR(128) NULL AFTER refund_reservation_id,
    ADD COLUMN workflow_instance_id VARCHAR(128) NULL AFTER idempotency_key,
    ADD COLUMN unknown_since DATETIME(3) NULL AFTER provider_refund_no,
    ADD COLUMN succeeded_at DATETIME(3) NULL AFTER unknown_since,
    ADD COLUMN failed_at DATETIME(3) NULL AFTER succeeded_at,
    ADD COLUMN created_by BIGINT NULL AFTER version,
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ADD COLUMN updated_by BIGINT NULL AFTER created_at,
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ADD UNIQUE KEY uk_refund_idem (tenant_id, idempotency_key),
    ADD KEY idx_refund_unknown (tenant_id, status, unknown_since, id);

ALTER TABLE refund_transaction
    ADD COLUMN channel VARCHAR(32) NULL AFTER refund_order_id,
    ADD COLUMN merchant_config_id BIGINT NULL AFTER channel,
    ADD COLUMN provider_status VARCHAR(64) NULL AFTER provider_refund_no,
    ADD COLUMN raw_payload_hash VARCHAR(128) NULL AFTER occurred_at,
    DROP INDEX uk_provider_refund,
    ADD UNIQUE KEY uk_provider_refund (channel, merchant_config_id, provider_refund_no);

CREATE TABLE refund_order_state_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    refund_order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    status_source VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NULL,
    evidence_type VARCHAR(32) NULL,
    evidence_id VARCHAR(128) NULL,
    trace_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_refund_state_order (tenant_id, refund_order_id, created_at)
);
