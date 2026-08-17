CREATE TABLE utility_tariff_plan (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tariff_code VARCHAR(64) NOT NULL,
    tariff_name VARCHAR(128) NOT NULL,
    utility_type VARCHAR(32) NOT NULL,
    pricing_mode VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    version_no INT NOT NULL,
    effective_from DATETIME(3) NOT NULL,
    effective_to DATETIME(3) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_tariff_code_ver (tenant_id, tariff_code, version_no),
    KEY idx_tariff_active (tenant_id, utility_type, status, effective_from, effective_to)
);

CREATE TABLE utility_tariff_tier (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tariff_plan_id BIGINT NOT NULL,
    tier_no INT NOT NULL,
    threshold_from DECIMAL(20,8) NOT NULL DEFAULT 0,
    threshold_to DECIMAL(20,8) NULL,
    time_bucket VARCHAR(32) NULL,
    unit_price DECIMAL(20,8) NOT NULL,
    UNIQUE KEY uk_tariff_tier (tenant_id, tariff_plan_id, tier_no, time_bucket)
);

ALTER TABLE billing_rule
    ADD COLUMN charge_basis_type VARCHAR(32) NULL AFTER calculation_type,
    ADD COLUMN charge_basis_value DECIMAL(20,6) NULL AFTER charge_basis_type,
    ADD COLUMN chargeable_area_snapshot DECIMAL(20,6) NULL AFTER charge_basis_value,
    ADD COLUMN utility_meter_id BIGINT NULL AFTER chargeable_area_snapshot,
    ADD COLUMN utility_tariff_plan_id BIGINT NULL AFTER utility_meter_id,
    ADD COLUMN source_reading_id BIGINT NULL AFTER utility_tariff_plan_id;

ALTER TABLE bill_item
    ADD COLUMN source_meter_reading_id BIGINT NULL AFTER source_rule_id,
    ADD COLUMN source_tariff_plan_id BIGINT NULL AFTER source_meter_reading_id,
    ADD COLUMN usage_quantity DECIMAL(20,6) NULL AFTER source_tariff_plan_id,
    ADD COLUMN usage_unit VARCHAR(16) NULL AFTER usage_quantity;

CREATE INDEX idx_bill_item_reading ON bill_item(tenant_id, source_meter_reading_id);
