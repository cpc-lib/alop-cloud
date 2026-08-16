ALTER TABLE customer
  ADD COLUMN email_verified_at DATETIME(3) NULL,
  ADD COLUMN phone_verified_at DATETIME(3) NULL;

ALTER TABLE customer_contact
  ADD COLUMN email_verified_at DATETIME(3) NULL,
  ADD COLUMN phone_verified_at DATETIME(3) NULL,
  ADD COLUMN is_billing_contact TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN receive_transactional_notification TINYINT NOT NULL DEFAULT 1;

CREATE INDEX idx_contact_billing
ON customer_contact(tenant_id, customer_id, is_billing_contact, status);
