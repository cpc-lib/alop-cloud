CREATE TABLE supplier (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  supplier_no VARCHAR(64) NOT NULL,
  supplier_name VARCHAR(256) NOT NULL,
  supplier_type VARCHAR(32) NOT NULL,
  tax_no VARCHAR(128) NULL,
  bank_name VARCHAR(256) NULL,
  bank_account_ciphertext VARCHAR(512) NULL,
  bank_account_hash VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_supplier_no (tenant_id, supplier_no),
  KEY idx_supplier_name (tenant_id, supplier_name, status)
);

CREATE TABLE payable (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  payable_no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  original_amount DECIMAL(18,2) NOT NULL,
  adjustment_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(18,2) NOT NULL,
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  outstanding_amount DECIMAL(18,2) NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payable_no (tenant_id, payable_no),
  KEY idx_payable_supplier (tenant_id, supplier_id, status, due_date)
);

CREATE TABLE payment_request (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  request_no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  requested_amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  workflow_instance_id VARCHAR(128) NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_request_no (tenant_id, request_no),
  KEY idx_payment_request_status (tenant_id, status, created_at)
);

CREATE TABLE payment_request_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  payment_request_id BIGINT NOT NULL,
  payable_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_request_item (tenant_id, payment_request_id, payable_id)
);
