CREATE TABLE property_owner (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  owner_no VARCHAR(64) NOT NULL,
  owner_type VARCHAR(32) NOT NULL,
  owner_name VARCHAR(256) NOT NULL,
  tax_no VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_owner_no (tenant_id, owner_no)
);

CREATE TABLE owner_operating_agreement (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  agreement_no VARCHAR(64) NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  effective_to DATETIME(3) NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_operating_agreement (tenant_id, agreement_no),
  KEY idx_owner_operating_asset (tenant_id, asset_id, status)
);

CREATE TABLE owner_settlement_rule (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  owner_operating_agreement_id BIGINT NOT NULL,
  rule_type VARCHAR(32) NOT NULL,
  rule_json JSON NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  effective_to DATETIME(3) NULL,
  version_no INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_settlement_rule_version (tenant_id, owner_operating_agreement_id, version_no)
);

CREATE TABLE owner_settlement_batch (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL,
  owner_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  gross_eligible_amount DECIMAL(18,2) NOT NULL,
  deduction_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(18,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_settlement_batch (tenant_id, batch_no),
  KEY idx_owner_settlement_owner (tenant_id, owner_id, status, period_end)
);

CREATE TABLE owner_settlement_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT NOT NULL,
  gross_amount DECIMAL(18,2) NOT NULL,
  deduction_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  settlement_amount DECIMAL(18,2) NOT NULL,
  calculation_trace_json JSON NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_settlement_source (tenant_id, batch_id, source_type, source_id)
);
