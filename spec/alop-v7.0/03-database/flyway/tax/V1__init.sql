CREATE TABLE tax_category (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tax_category (tenant_id, category_code)
);

CREATE TABLE tax_rule (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  jurisdiction_code VARCHAR(64) NOT NULL,
  tax_category_code VARCHAR(64) NOT NULL,
  tax_mode VARCHAR(32) NOT NULL,
  tax_rate DECIMAL(20,8) NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  effective_to DATETIME(3) NULL,
  version_no INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tax_rule_version (tenant_id, jurisdiction_code, tax_category_code, version_no),
  KEY idx_tax_rule_active (tenant_id, jurisdiction_code, tax_category_code, status, effective_from, effective_to)
);
