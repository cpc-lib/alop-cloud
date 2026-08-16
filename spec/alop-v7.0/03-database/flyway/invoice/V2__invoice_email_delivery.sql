ALTER TABLE invoice_application
  ADD COLUMN delivery_mode VARCHAR(32) NOT NULL DEFAULT 'NONE',
  ADD COLUMN delivery_email_ciphertext VARCHAR(1024) NULL,
  ADD COLUMN delivery_email_hash VARCHAR(128) NULL;

CREATE TABLE invoice_delivery_instruction (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, invoice_id BIGINT NOT NULL,
  application_id BIGINT NOT NULL, delivery_type VARCHAR(32) NOT NULL,
  source VARCHAR(32) NOT NULL, parent_instruction_id BIGINT NULL,
  template_code VARCHAR(64) NOT NULL, subject_snapshot VARCHAR(512) NULL,
  recipient_set_hash VARCHAR(128) NOT NULL, dedup_key VARCHAR(256) NULL, status VARCHAR(32) NOT NULL,
  notification_message_id BIGINT NULL, requested_by BIGINT NULL,
  requested_at DATETIME(3) NOT NULL, sent_at DATETIME(3) NULL,
  failure_code VARCHAR(128) NULL, failure_message VARCHAR(512) NULL,
  version INT NOT NULL DEFAULT 0, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  KEY idx_invoice_delivery_invoice(tenant_id, invoice_id, requested_at),
  KEY idx_invoice_delivery_status(tenant_id, status, requested_at),
  UNIQUE KEY uk_invoice_delivery_dedup(tenant_id, dedup_key)
);

CREATE TABLE invoice_delivery_recipient (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, instruction_id BIGINT NOT NULL,
  recipient_type VARCHAR(16) NOT NULL, email_ciphertext VARCHAR(1024) NOT NULL,
  email_hash VARCHAR(128) NOT NULL, display_name VARCHAR(256) NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_invoice_delivery_recipient(tenant_id, instruction_id),
  KEY idx_invoice_delivery_email_hash(tenant_id, email_hash)
);

-- AUTO_AFTER_ISSUE writes a deterministic non-null dedup_key; MANUAL_RESEND leaves dedup_key NULL so multiple audited resends are allowed.
