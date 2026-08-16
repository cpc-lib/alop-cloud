CREATE TABLE notification_rule (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, rule_code VARCHAR(64) NOT NULL,
  business_event_type VARCHAR(128) NOT NULL, category VARCHAR(32) NOT NULL,
  recipient_strategy VARCHAR(64) NOT NULL, channel_json JSON NOT NULL,
  template_mapping_json JSON NOT NULL, fallback_policy VARCHAR(32) NOT NULL DEFAULT 'NONE',
  quiet_hour_policy VARCHAR(32) NOT NULL DEFAULT 'DEFAULT', priority INT NOT NULL DEFAULT 100,
  status VARCHAR(32) NOT NULL, version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_rule(tenant_id, rule_code),
  KEY idx_notification_rule_event(tenant_id, business_event_type, status)
);

CREATE TABLE notification_template (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, template_code VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL, category VARCHAR(32) NOT NULL, version_no INT NOT NULL,
  subject_template VARCHAR(512) NULL, body_template MEDIUMTEXT NOT NULL,
  plain_text_template MEDIUMTEXT NULL, provider_template_ref VARCHAR(256) NULL,
  variable_schema_json JSON NULL, status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_template(tenant_id, template_code, channel, version_no),
  KEY idx_notification_template_active(tenant_id, template_code, channel, status)
);

CREATE TABLE notification_message (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, message_no VARCHAR(64) NOT NULL,
  category VARCHAR(32) NOT NULL, business_type VARCHAR(64) NOT NULL, business_id BIGINT NOT NULL,
  business_event_id VARCHAR(64) NULL, rule_code VARCHAR(64) NOT NULL, trigger_key VARCHAR(128) NOT NULL,
  recipient_ref_type VARCHAR(32) NOT NULL, recipient_ref_id BIGINT NULL,
  status VARCHAR(32) NOT NULL, priority INT NOT NULL DEFAULT 100,
  scheduled_at DATETIME(3) NULL, completed_at DATETIME(3) NULL,
  dedup_key VARCHAR(256) NOT NULL, trace_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_message_dedup(tenant_id, dedup_key),
  UNIQUE KEY uk_notification_message_no(tenant_id, message_no),
  KEY idx_notification_message_status(tenant_id, status, scheduled_at)
);

CREATE TABLE notification_delivery (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, notification_message_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL, template_code VARCHAR(64) NOT NULL, template_version INT NOT NULL,
  recipient_type VARCHAR(16) NOT NULL DEFAULT 'TO', recipient_address_ciphertext VARCHAR(1024) NOT NULL,
  recipient_address_hash VARCHAR(128) NOT NULL, provider_code VARCHAR(64) NULL,
  provider_message_id VARCHAR(256) NULL, provider_request_no VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL, result_uncertain TINYINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0, next_retry_at DATETIME(3) NULL,
  content_hash VARCHAR(128) NULL, last_error_code VARCHAR(128) NULL, last_error_message VARCHAR(512) NULL,
  sent_at DATETIME(3) NULL, delivered_at DATETIME(3) NULL,
  version INT NOT NULL DEFAULT 0, created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_delivery_req(provider_request_no),
  UNIQUE KEY uk_notification_delivery_dedup(tenant_id, notification_message_id, channel, recipient_address_hash, template_version),
  KEY idx_notification_delivery_retry(tenant_id, status, next_retry_at),
  KEY idx_notification_delivery_provider(tenant_id, provider_code, provider_message_id)
);

CREATE TABLE notification_delivery_attempt (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, delivery_id BIGINT NOT NULL,
  attempt_no INT NOT NULL, provider_request_no VARCHAR(128) NOT NULL,
  result_type VARCHAR(32) NOT NULL, provider_http_status INT NULL,
  provider_error_code VARCHAR(128) NULL, duration_ms BIGINT NULL,
  attempted_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_attempt(tenant_id, delivery_id, attempt_no),
  UNIQUE KEY uk_notification_attempt_req(provider_request_no)
);

CREATE TABLE notification_recipient_preference (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, subject_type VARCHAR(32) NOT NULL, subject_id BIGINT NOT NULL,
  marketing_email_enabled TINYINT NOT NULL DEFAULT 1, marketing_sms_enabled TINYINT NOT NULL DEFAULT 1,
  transactional_email_enabled TINYINT NOT NULL DEFAULT 1, transactional_sms_enabled TINYINT NOT NULL DEFAULT 1,
  preferred_channel VARCHAR(32) NULL, timezone VARCHAR(64) NULL,
  version INT NOT NULL DEFAULT 0, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_pref(tenant_id, subject_type, subject_id)
);

CREATE TABLE notification_provider_config (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
  provider_mode VARCHAR(32) NOT NULL, provider_code VARCHAR(64) NOT NULL,
  sender_identity VARCHAR(256) NULL, credential_ref VARCHAR(512) NOT NULL,
  callback_secret_ref VARCHAR(512) NULL, config_json JSON NULL,
  status VARCHAR(32) NOT NULL, version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_notification_provider(tenant_id, channel, provider_code)
);

CREATE TABLE notification_suppression (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
  recipient_address_hash VARCHAR(128) NOT NULL, reason_type VARCHAR(32) NOT NULL,
  reason VARCHAR(512) NULL, effective_from DATETIME(3) NOT NULL, effective_to DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL,
  KEY idx_notification_suppress(tenant_id, channel, recipient_address_hash, effective_from, effective_to)
);
