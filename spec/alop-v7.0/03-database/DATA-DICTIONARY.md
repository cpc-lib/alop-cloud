# Database Data Dictionary — V7.0

Total parsed tables: **141**.

> Canonical executable schema remains the Flyway SQL. This document is generated for code review/codegen navigation.

## agreement

### `agreement`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_no` | `VARCHAR(64)` | NO | `None` |
| `agreement_type` | `VARCHAR(32)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `source_opportunity_id` | `BIGINT` | YES | `None` |
| `source_quotation_version_id` | `BIGINT` | NO | `None` |
| `source_reservation_id` | `BIGINT` | NO | `None` |
| `previous_agreement_id` | `BIGINT` | YES | `None` |
| `sign_time` | `DATETIME(3)` | YES | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `total_amount` | `DECIMAL(18,2)` | NO | `None` |
| `deposit_amount` | `DECIMAL(18,2)` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `expiry_stage` | `VARCHAR(16)` | NO | `'NONE'` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_agreement` (tenant_id, agreement_no)
- KEY `idx_ag_customer` (tenant_id, customer_id, status)
- KEY `idx_ag_expiry` (tenant_id, status, end_time, id)

### `agreement_item`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `offering_id` | `BIGINT` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `unit_price` | `DECIMAL(20,8)` | NO | `None` |
| `quantity` | `DECIMAL(20,6)` | NO | `1` |
| `discount_amount` | `DECIMAL(18,2)` | NO | `0` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_ag_item_res` (tenant_id, resource_unit_id, start_time, end_time)

### `agreement_snapshot`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `snapshot_version` | `INT` | NO | `None` |
| `snapshot_type` | `VARCHAR(32)` | NO | `None` |
| `payload` | `JSON` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_ag_snapshot` (tenant_id, agreement_id, snapshot_version, snapshot_type)

### `agreement_change`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `change_no` | `VARCHAR(64)` | NO | `None` |
| `change_type` | `VARCHAR(32)` | NO | `None` |
| `effective_date` | `DATETIME(3)` | NO | `None` |
| `reason` | `VARCHAR(1024)` | YES | `None` |
| `before_snapshot` | `JSON` | NO | `None` |
| `after_snapshot` | `JSON` | NO | `None` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_ag_change` (tenant_id, change_no)
- KEY `idx_ag_change_ag` (tenant_id, agreement_id, status)

### `agreement_sign_process`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `reservation_id` | `BIGINT` | NO | `None` |
| `process_status` | `VARCHAR(32)` | NO | `None` |
| `asset_commit_status` | `VARCHAR(32)` | NO | `None` |
| `agreement_commit_status` | `VARCHAR(32)` | NO | `None` |
| `retry_count` | `INT` | NO | `0` |
| `last_error` | `TEXT` | YES | `None` |
| `request_id` | `VARCHAR(128)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_sign_request` (tenant_id, request_id)
- UNIQUE KEY `uk_sign_agreement` (tenant_id, agreement_id)

### `renewal_priority`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `priority_start` | `DATETIME(3)` | NO | `None` |
| `priority_end` | `DATETIME(3)` | NO | `None` |
| `priority_mode` | `VARCHAR(16)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `decision_at` | `DATETIME(3)` | YES | `None` |
| `decision_by` | `BIGINT` | YES | `None` |
| `decision_reason` | `VARCHAR(512)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_renewal_res` (tenant_id, resource_unit_id, status, priority_end)

### `handover_order`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `handover_no` | `VARCHAR(64)` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `handover_type` | `VARCHAR(16)` | NO | `None` |
| `planned_at` | `DATETIME(3)` | YES | `None` |
| `completed_at` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `signed_file_id` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_handover` (tenant_id, handover_no)
- KEY `idx_handover_ag` (tenant_id, agreement_id, status)

### `handover_item`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `handover_order_id` | `BIGINT` | NO | `None` |
| `item_type` | `VARCHAR(32)` | NO | `None` |
| `item_name` | `VARCHAR(128)` | NO | `None` |
| `before_value` | `VARCHAR(512)` | YES | `None` |
| `after_value` | `VARCHAR(512)` | YES | `None` |
| `amount` | `DECIMAL(18,2)` | YES | `None` |
| `evidence_file_id` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)

### `signature_process`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `provider` | `VARCHAR(32)` | NO | `None` |
| `provider_process_no` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `signed_file_id` | `BIGINT` | YES | `None` |
| `file_sha256` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_sig_ag` (tenant_id, agreement_id)

### `parking_vehicle_binding`

Migration: `03-database/flyway/agreement/V2__parking_vehicle_binding.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `agreement_item_id` | `BIGINT` | NO | `None` |
| `parking_resource_id` | `BIGINT` | NO | `None` |
| `vehicle_id` | `BIGINT` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_parking_binding_agreement` (tenant_id, agreement_id, status)
- KEY `idx_parking_binding_resource` (tenant_id, parking_resource_id, status, effective_from, effective_to)
- KEY `idx_parking_binding_vehicle` (tenant_id, vehicle_id, status)

### `agreement_party`

Migration: `03-database/flyway/agreement/V3__agreement_party_resource_transfer.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `party_role` | `VARCHAR(32)` | NO | `None` |
| `party_type` | `VARCHAR(32)` | NO | `None` |
| `customer_id` | `BIGINT` | YES | `None` |
| `organization_id` | `BIGINT` | YES | `None` |
| `external_party_ref` | `VARCHAR(128)` | YES | `None` |
| `legal_name_snapshot` | `VARCHAR(256)` | NO | `None` |
| `unified_credit_code_snapshot` | `VARCHAR(64)` | YES | `None` |
| `id_number_masked_snapshot` | `VARCHAR(128)` | YES | `None` |
| `contact_name_snapshot` | `VARCHAR(128)` | YES | `None` |
| `phone_masked_snapshot` | `VARCHAR(64)` | YES | `None` |
| `email_masked_snapshot` | `VARCHAR(256)` | YES | `None` |
| `address_snapshot` | `VARCHAR(512)` | YES | `None` |
| `bank_name_snapshot` | `VARCHAR(256)` | YES | `None` |
| `bank_account_masked_snapshot` | `VARCHAR(128)` | YES | `None` |
| `tax_no_snapshot` | `VARCHAR(128)` | YES | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_by` | `BIGINT` | YES | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_agreement_party_active` (tenant_id, agreement_id, party_role, status, effective_from)

### `resource_transfer`

Migration: `03-database/flyway/agreement/V3__agreement_party_resource_transfer.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `transfer_no` | `VARCHAR(64)` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `from_resource_unit_id` | `BIGINT` | NO | `None` |
| `to_resource_unit_id` | `BIGINT` | NO | `None` |
| `requested_effective_at` | `DATETIME(3)` | NO | `None` |
| `actual_effective_at` | `DATETIME(3)` | YES | `None` |
| `reason_type` | `VARCHAR(32)` | NO | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `price_difference` | `DECIMAL(18,2)` | NO | `0` |
| `deposit_difference` | `DECIMAL(18,2)` | NO | `0` |
| `billing_impact_status` | `VARCHAR(32)` | NO | `'PENDING'` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_by` | `BIGINT` | YES | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_resource_transfer_no` (tenant_id, transfer_no)
- KEY `idx_resource_transfer_agreement` (tenant_id, agreement_id, status)
- KEY `idx_resource_transfer_target` (tenant_id, to_resource_unit_id, requested_effective_at)

## ap

### `supplier`

Migration: `03-database/flyway/ap/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `supplier_no` | `VARCHAR(64)` | NO | `None` |
| `supplier_name` | `VARCHAR(256)` | NO | `None` |
| `supplier_type` | `VARCHAR(32)` | NO | `None` |
| `tax_no` | `VARCHAR(128)` | YES | `None` |
| `bank_name` | `VARCHAR(256)` | YES | `None` |
| `bank_account_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `bank_account_hash` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_supplier_no` (tenant_id, supplier_no)
- KEY `idx_supplier_name` (tenant_id, supplier_name, status)

### `payable`

Migration: `03-database/flyway/ap/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payable_no` | `VARCHAR(64)` | NO | `None` |
| `supplier_id` | `BIGINT` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `original_amount` | `DECIMAL(18,2)` | NO | `None` |
| `adjustment_amount` | `DECIMAL(18,2)` | NO | `0` |
| `payable_amount` | `DECIMAL(18,2)` | NO | `None` |
| `paid_amount` | `DECIMAL(18,2)` | NO | `0` |
| `outstanding_amount` | `DECIMAL(18,2)` | NO | `None` |
| `due_date` | `DATE` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_payable_no` (tenant_id, payable_no)
- KEY `idx_payable_supplier` (tenant_id, supplier_id, status, due_date)

### `payment_request`

Migration: `03-database/flyway/ap/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `request_no` | `VARCHAR(64)` | NO | `None` |
| `supplier_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `requested_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_payment_request_no` (tenant_id, request_no)
- KEY `idx_payment_request_status` (tenant_id, status, created_at)

### `payment_request_item`

Migration: `03-database/flyway/ap/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payment_request_id` | `BIGINT` | NO | `None` |
| `payable_id` | `BIGINT` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_payment_request_item` (tenant_id, payment_request_id, payable_id)

## asset

### `asset`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `asset_code` | `VARCHAR(64)` | NO | `None` |
| `asset_name` | `VARCHAR(128)` | NO | `None` |
| `asset_type` | `VARCHAR(32)` | NO | `None` |
| `ownership_type` | `VARCHAR(32)` | NO | `None` |
| `operation_mode` | `VARCHAR(32)` | NO | `None` |
| `country_code` | `VARCHAR(8)` | YES | `None` |
| `province_code` | `VARCHAR(32)` | YES | `None` |
| `city_code` | `VARCHAR(32)` | YES | `None` |
| `district_code` | `VARCHAR(32)` | YES | `None` |
| `address` | `VARCHAR(512)` | YES | `None` |
| `longitude` | `DECIMAL(10,7)` | YES | `None` |
| `latitude` | `DECIMAL(10,7)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `management_team_id` | `BIGINT` | YES | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_by` | `BIGINT` | YES | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `deleted` | `TINYINT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_asset` (tenant_id, asset_code)
- KEY `idx_asset_status` (tenant_id, status)
- KEY `idx_asset_city` (tenant_id, city_code)

### `asset_space`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `asset_id` | `BIGINT` | NO | `None` |
| `parent_space_id` | `BIGINT` | YES | `None` |
| `space_code` | `VARCHAR(64)` | NO | `None` |
| `space_name` | `VARCHAR(128)` | NO | `None` |
| `space_type` | `VARCHAR(32)` | NO | `None` |
| `level_no` | `INT` | YES | `None` |
| `sort_order` | `INT` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `deleted` | `TINYINT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_space` (tenant_id, asset_id, space_code)
- KEY `idx_space_parent` (tenant_id, parent_space_id)

### `resource_unit`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `asset_id` | `BIGINT` | NO | `None` |
| `space_id` | `BIGINT` | YES | `None` |
| `resource_code` | `VARCHAR(64)` | NO | `None` |
| `resource_name` | `VARCHAR(128)` | NO | `None` |
| `resource_type` | `VARCHAR(32)` | NO | `None` |
| `area` | `DECIMAL(12,2)` | YES | `None` |
| `capacity` | `INT` | YES | `None` |
| `rentable` | `TINYINT` | NO | `1` |
| `sellable` | `TINYINT` | NO | `0` |
| `physical_status` | `VARCHAR(32)` | NO | `None` |
| `sale_status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `deleted` | `TINYINT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_resource` (tenant_id, resource_code)
- KEY `idx_res_asset` (tenant_id, asset_id)
- KEY `idx_res_status` (tenant_id, physical_status, sale_status)

### `resource_conflict_group`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `group_code` | `VARCHAR(64)` | NO | `None` |
| `group_type` | `VARCHAR(32)` | NO | `None` |
| `name` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_conflict_group` (tenant_id, group_code)

### `resource_conflict_group_member`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |
| `group_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |

Indexes:
- PRIMARY KEY `` (tenant_id, group_id, resource_unit_id)
- KEY `idx_conf_member_resource` (tenant_id, resource_unit_id)

### `valuation`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `valuation_no` | `VARCHAR(64)` | NO | `None` |
| `asset_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | YES | `None` |
| `valuation_version` | `INT` | NO | `None` |
| `suggested_rent` | `DECIMAL(18,2)` | YES | `None` |
| `floor_rent` | `DECIMAL(18,2)` | YES | `None` |
| `suggested_sale_price` | `DECIMAL(18,2)` | YES | `None` |
| `floor_sale_price` | `DECIMAL(18,2)` | YES | `None` |
| `valid_from` | `DATETIME(3)` | NO | `None` |
| `valid_until` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `report_file_id` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_valuation` (tenant_id, valuation_no)
- KEY `idx_valuation_resource` (tenant_id, resource_unit_id, valuation_version)

### `offering`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `offering_code` | `VARCHAR(64)` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `source_valuation_id` | `BIGINT` | YES | `None` |
| `offering_name` | `VARCHAR(128)` | NO | `None` |
| `offering_type` | `VARCHAR(32)` | NO | `None` |
| `pricing_model` | `VARCHAR(32)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `base_price` | `DECIMAL(18,2)` | YES | `None` |
| `guidance_price` | `DECIMAL(18,2)` | YES | `None` |
| `minimum_price` | `DECIMAL(18,2)` | YES | `None` |
| `valid_from` | `DATETIME(3)` | NO | `None` |
| `valid_to` | `DATETIME(3)` | YES | `None` |
| `minimum_duration_minutes` | `BIGINT` | YES | `None` |
| `maximum_duration_minutes` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_offering` (tenant_id, offering_code)
- KEY `idx_offer_res` (tenant_id, resource_unit_id, status)

### `listing`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `listing_no` | `VARCHAR(64)` | NO | `None` |
| `offering_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `title` | `VARCHAR(256)` | NO | `None` |
| `description` | `TEXT` | YES | `None` |
| `display_price` | `DECIMAL(18,2)` | YES | `None` |
| `publish_start` | `DATETIME(3)` | YES | `None` |
| `publish_end` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `data_version` | `BIGINT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_listing` (tenant_id, listing_no)
- KEY `idx_listing_channel` (tenant_id, channel, status)
- KEY `idx_listing_res` (tenant_id, resource_unit_id)

### `renovation_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `renovation_no` | `VARCHAR(64)` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_reno` (tenant_id, renovation_no)
- KEY `idx_reno_res` (tenant_id, resource_unit_id, status)

### `maintenance_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `maintenance_no` | `VARCHAR(64)` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `emergency` | `TINYINT` | NO | `0` |
| `planned_start` | `DATETIME(3)` | YES | `None` |
| `planned_end` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `problem` | `TEXT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_maint` (tenant_id, maintenance_no)

### `operation_work_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `work_order_no` | `VARCHAR(64)` | NO | `None` |
| `work_order_type` | `VARCHAR(32)` | NO | `None` |
| `resource_unit_id` | `BIGINT` | YES | `None` |
| `customer_id` | `BIGINT` | YES | `None` |
| `agreement_id` | `BIGINT` | YES | `None` |
| `priority` | `VARCHAR(16)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `assignee_id` | `BIGINT` | YES | `None` |
| `sla_due_at` | `DATETIME(3)` | YES | `None` |
| `description` | `TEXT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_workorder` (tenant_id, work_order_no)
- KEY `idx_workorder_status` (tenant_id, status, sla_due_at)

### `parking_space_profile`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `parking_no` | `VARCHAR(64)` | NO | `None` |
| `zone` | `VARCHAR(64)` | YES | `None` |
| `floor_no` | `VARCHAR(32)` | YES | `None` |
| `parking_type` | `VARCHAR(32)` | NO | `None` |
| `indoor` | `TINYINT` | NO | `1` |
| `charging_supported` | `TINYINT` | NO | `0` |
| `charger_meter_id` | `BIGINT` | YES | `None` |
| `vehicle_size_limit` | `VARCHAR(32)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_parking_profile_resource` (tenant_id, resource_unit_id)
- UNIQUE KEY `uk_parking_no` (tenant_id, parking_no)
- KEY `idx_parking_zone` (tenant_id, zone, status)

### `utility_meter`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_no` | `VARCHAR(64)` | NO | `None` |
| `utility_type` | `VARCHAR(32)` | NO | `None` |
| `unit_code` | `VARCHAR(16)` | NO | `None` |
| `meter_scope` | `VARCHAR(32)` | NO | `None` |
| `reading_mode` | `VARCHAR(32)` | NO | `None` |
| `multiplier` | `DECIMAL(20,8)` | NO | `1` |
| `manufacturer` | `VARCHAR(128)` | YES | `None` |
| `serial_no` | `VARCHAR(128)` | YES | `None` |
| `installed_at` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_utility_meter_no` (tenant_id, meter_no)
- KEY `idx_utility_meter_type` (tenant_id, utility_type, status)

### `utility_meter_binding`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_id` | `BIGINT` | NO | `None` |
| `asset_id` | `BIGINT` | YES | `None` |
| `space_id` | `BIGINT` | YES | `None` |
| `resource_unit_id` | `BIGINT` | YES | `None` |
| `allocation_method` | `VARCHAR(32)` | NO | `None` |
| `allocation_ratio` | `DECIMAL(20,8)` | YES | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_meter_binding_meter` (tenant_id, meter_id, status, effective_from, effective_to)
- KEY `idx_meter_binding_resource` (tenant_id, resource_unit_id, status, effective_from, effective_to)

### `utility_meter_reading`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_id` | `BIGINT` | NO | `None` |
| `reading_no` | `VARCHAR(64)` | NO | `None` |
| `period_start` | `DATETIME(3)` | NO | `None` |
| `period_end` | `DATETIME(3)` | NO | `None` |
| `previous_reading` | `DECIMAL(20,6)` | NO | `None` |
| `current_reading` | `DECIMAL(20,6)` | NO | `None` |
| `consumption` | `DECIMAL(20,6)` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `version_no` | `INT` | NO | `1` |
| `supersedes_reading_id` | `BIGINT` | YES | `None` |
| `handover_order_id` | `BIGINT` | YES | `None` |
| `evidence_file_id` | `BIGINT` | YES | `None` |
| `anomaly_code` | `VARCHAR(64)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `submitted_by` | `BIGINT` | YES | `None` |
| `submitted_at` | `DATETIME(3)` | YES | `None` |
| `verified_by` | `BIGINT` | YES | `None` |
| `verified_at` | `DATETIME(3)` | YES | `None` |
| `billed_at` | `DATETIME(3)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_meter_reading_no` (tenant_id, reading_no)
- UNIQUE KEY `uk_meter_period_version` (tenant_id, meter_id, period_start, period_end, version_no)
- KEY `idx_meter_reading_period` (tenant_id, meter_id, status, period_start, period_end)
- KEY `idx_meter_reading_handover` (tenant_id, handover_order_id)

## billing

### `billing_rule`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `agreement_item_id` | `BIGINT` | YES | `None` |
| `rule_version` | `INT` | NO | `None` |
| `charge_type` | `VARCHAR(32)` | NO | `None` |
| `calculation_type` | `VARCHAR(32)` | NO | `None` |
| `billing_cycle_months` | `INT` | YES | `None` |
| `start_date` | `DATE` | NO | `None` |
| `end_date` | `DATE` | NO | `None` |
| `unit_price` | `DECIMAL(20,8)` | NO | `None` |
| `quantity` | `DECIMAL(20,6)` | NO | `1` |
| `proration_method` | `VARCHAR(32)` | NO | `'NONE'` |
| `free_days` | `INT` | NO | `0` |
| `escalation_type` | `VARCHAR(32)` | NO | `'NONE'` |
| `escalation_value` | `DECIMAL(20,8)` | YES | `None` |
| `due_rule_type` | `VARCHAR(32)` | NO | `None` |
| `due_rule_value` | `INT` | YES | `None` |
| `priority` | `INT` | NO | `100` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `calculation_config` | `JSON` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_bill_rule_ver` (tenant_id, agreement_id, id, rule_version)
- KEY `idx_bill_rule_ag` (tenant_id, agreement_id, status)

### `billing_plan`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `plan_version` | `INT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `generated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_plan_ver` (tenant_id, agreement_id, plan_version)

### `billing_plan_item`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `billing_plan_id` | `BIGINT` | NO | `None` |
| `source_rule_id` | `BIGINT` | NO | `None` |
| `charge_type` | `VARCHAR(32)` | NO | `None` |
| `period_start` | `DATETIME(3)` | NO | `None` |
| `period_end` | `DATETIME(3)` | NO | `None` |
| `due_date` | `DATE` | NO | `None` |
| `planned_amount` | `DECIMAL(18,2)` | NO | `None` |
| `calculation_trace` | `JSON` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_plan_item_due` (tenant_id, due_date)

### `bill`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `bill_no` | `VARCHAR(64)` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `period_start` | `DATETIME(3)` | NO | `None` |
| `period_end` | `DATETIME(3)` | NO | `None` |
| `original_amount` | `DECIMAL(18,2)` | NO | `None` |
| `adjustment_amount` | `DECIMAL(18,2)` | NO | `0` |
| `payable_amount` | `DECIMAL(18,2)` | NO | `None` |
| `due_date` | `DATE` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `issued_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_bill` (tenant_id, bill_no)
- KEY `idx_bill_ag` (tenant_id, agreement_id, status)
- KEY `idx_bill_due` (tenant_id, status, due_date)

### `bill_item`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `bill_id` | `BIGINT` | NO | `None` |
| `charge_type` | `VARCHAR(32)` | NO | `None` |
| `description` | `VARCHAR(512)` | YES | `None` |
| `quantity` | `DECIMAL(20,6)` | NO | `None` |
| `unit_price` | `DECIMAL(20,8)` | NO | `None` |
| `original_amount` | `DECIMAL(18,2)` | NO | `None` |
| `discount_amount` | `DECIMAL(18,2)` | NO | `0` |
| `final_amount` | `DECIMAL(18,2)` | NO | `None` |
| `source_rule_id` | `BIGINT` | NO | `None` |
| `calculation_trace` | `JSON` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_bill_item_bill` (tenant_id, bill_id)

### `utility_tariff_plan`

Migration: `03-database/flyway/billing/V2__utility_property_parking_billing.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `tariff_code` | `VARCHAR(64)` | NO | `None` |
| `tariff_name` | `VARCHAR(128)` | NO | `None` |
| `utility_type` | `VARCHAR(32)` | NO | `None` |
| `pricing_mode` | `VARCHAR(32)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `unit_code` | `VARCHAR(16)` | NO | `None` |
| `version_no` | `INT` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tariff_code_ver` (tenant_id, tariff_code, version_no)
- KEY `idx_tariff_active` (tenant_id, utility_type, status, effective_from, effective_to)

### `utility_tariff_tier`

Migration: `03-database/flyway/billing/V2__utility_property_parking_billing.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `tariff_plan_id` | `BIGINT` | NO | `None` |
| `tier_no` | `INT` | NO | `None` |
| `threshold_from` | `DECIMAL(20,8)` | NO | `0` |
| `threshold_to` | `DECIMAL(20,8)` | YES | `None` |
| `time_bucket` | `VARCHAR(32)` | YES | `None` |
| `unit_price` | `DECIMAL(20,8)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tariff_tier` (tenant_id, tariff_plan_id, tier_no, time_bucket)

### `utility_usage_period`

Migration: `03-database/flyway/billing/V3__utility_usage_period.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `agreement_item_id` | `BIGINT` | YES | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `meter_id` | `BIGINT` | NO | `None` |
| `utility_type` | `VARCHAR(32)` | NO | `None` |
| `period_start` | `DATETIME(3)` | NO | `None` |
| `period_end` | `DATETIME(3)` | NO | `None` |
| `start_reading_id` | `BIGINT` | NO | `None` |
| `end_reading_id` | `BIGINT` | NO | `None` |
| `start_value` | `DECIMAL(20,6)` | NO | `None` |
| `end_value` | `DECIMAL(20,6)` | NO | `None` |
| `meter_multiplier` | `DECIMAL(20,8)` | NO | `1` |
| `raw_usage` | `DECIMAL(20,6)` | NO | `None` |
| `adjusted_usage` | `DECIMAL(20,6)` | NO | `None` |
| `allocation_method` | `VARCHAR(32)` | NO | `None` |
| `allocation_factor` | `DECIMAL(20,8)` | YES | `None` |
| `billable_usage` | `DECIMAL(20,6)` | NO | `None` |
| `tariff_plan_id` | `BIGINT` | NO | `None` |
| `tariff_version` | `INT` | NO | `None` |
| `estimated_flag` | `TINYINT` | NO | `0` |
| `correction_of_usage_period_id` | `BIGINT` | YES | `None` |
| `calculation_trace_json` | `JSON` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_usage_period_agreement` (tenant_id, agreement_id, utility_type, period_start)
- KEY `idx_usage_period_meter` (tenant_id, meter_id, period_start, period_end)
- KEY `idx_usage_period_status` (tenant_id, status, period_end)

## crm

### `crm_lead`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `lead_no` | `VARCHAR(64)` | NO | `None` |
| `source` | `VARCHAR(32)` | NO | `None` |
| `name` | `VARCHAR(128)` | YES | `None` |
| `phone_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `phone_hash` | `VARCHAR(128)` | YES | `None` |
| `email` | `VARCHAR(256)` | YES | `None` |
| `company` | `VARCHAR(256)` | YES | `None` |
| `requirement` | `TEXT` | YES | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `owner_team_id` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `first_contact_due_at` | `DATETIME(3)` | YES | `None` |
| `first_contact_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_lead` (tenant_id, lead_no)
- KEY `idx_lead_owner` (tenant_id, owner_user_id, status)
- KEY `idx_lead_phone` (tenant_id, phone_hash)

### `customer`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_no` | `VARCHAR(64)` | NO | `None` |
| `customer_type` | `VARCHAR(32)` | NO | `None` |
| `customer_name` | `VARCHAR(256)` | NO | `None` |
| `phone_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `phone_hash` | `VARCHAR(128)` | YES | `None` |
| `email` | `VARCHAR(256)` | YES | `None` |
| `company_name` | `VARCHAR(256)` | YES | `None` |
| `source` | `VARCHAR(32)` | YES | `None` |
| `customer_level` | `VARCHAR(32)` | YES | `None` |
| `credit_level` | `VARCHAR(32)` | YES | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `owner_team_id` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `merged_to_customer_id` | `BIGINT` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_customer` (tenant_id, customer_no)
- KEY `idx_customer_owner` (tenant_id, owner_user_id, status)
- KEY `idx_customer_phone` (tenant_id, phone_hash)

### `customer_contact`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `name` | `VARCHAR(128)` | NO | `None` |
| `phone_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `phone_hash` | `VARCHAR(128)` | YES | `None` |
| `email` | `VARCHAR(256)` | YES | `None` |
| `position_name` | `VARCHAR(128)` | YES | `None` |
| `is_primary` | `TINYINT` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_contact_customer` (tenant_id, customer_id)

### `customer_merge_record`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `source_customer_id` | `BIGINT` | NO | `None` |
| `target_customer_id` | `BIGINT` | NO | `None` |
| `merge_snapshot` | `JSON` | NO | `None` |
| `merged_by` | `BIGINT` | NO | `None` |
| `merged_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_merge_source` (tenant_id, source_customer_id)

### `customer_ownership_history`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `old_owner_id` | `BIGINT` | YES | `None` |
| `new_owner_id` | `BIGINT` | YES | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `changed_by` | `BIGINT` | NO | `None` |
| `changed_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_cust_owner_hist` (tenant_id, customer_id, changed_at)

### `opportunity`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `opportunity_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `opportunity_type` | `VARCHAR(32)` | NO | `None` |
| `budget_min` | `DECIMAL(18,2)` | YES | `None` |
| `budget_max` | `DECIMAL(18,2)` | YES | `None` |
| `required_area_min` | `DECIMAL(12,2)` | YES | `None` |
| `required_area_max` | `DECIMAL(12,2)` | YES | `None` |
| `expected_start` | `DATETIME(3)` | YES | `None` |
| `expected_end` | `DATETIME(3)` | YES | `None` |
| `preferred_region` | `VARCHAR(256)` | YES | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `stage` | `VARCHAR(32)` | NO | `None` |
| `stage_enter_at` | `DATETIME(3)` | NO | `None` |
| `lost_reason_code` | `VARCHAR(64)` | YES | `None` |
| `lost_reason` | `VARCHAR(512)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_opp` (tenant_id, opportunity_no)
- KEY `idx_opp_owner` (tenant_id, owner_user_id, stage)

### `opportunity_stage_history`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `opportunity_id` | `BIGINT` | NO | `None` |
| `from_stage` | `VARCHAR(32)` | YES | `None` |
| `to_stage` | `VARCHAR(32)` | NO | `None` |
| `changed_by` | `BIGINT` | NO | `None` |
| `changed_at` | `DATETIME(3)` | NO | `None` |
| `duration_seconds` | `BIGINT` | YES | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_opp_hist` (tenant_id, opportunity_id, changed_at)

### `viewing_appointment`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `viewing_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `opportunity_id` | `BIGINT` | YES | `None` |
| `appointment_time` | `DATETIME(3)` | NO | `None` |
| `advisor_id` | `BIGINT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `arrived_at` | `DATETIME(3)` | YES | `None` |
| `completed_at` | `DATETIME(3)` | YES | `None` |
| `customer_feedback` | `TEXT` | YES | `None` |
| `intention_level` | `VARCHAR(32)` | YES | `None` |
| `advisor_comment` | `TEXT` | YES | `None` |
| `next_follow_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_viewing` (tenant_id, viewing_no)

### `viewing_resource_relation`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |
| `viewing_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |

Indexes:
- PRIMARY KEY `` (tenant_id, viewing_id, resource_unit_id)

### `quotation`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `quotation_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `opportunity_id` | `BIGINT` | NO | `None` |
| `current_version_no` | `INT` | NO | `1` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_quote` (tenant_id, quotation_no)

### `quotation_version`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `quotation_id` | `BIGINT` | NO | `None` |
| `version_no` | `INT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `valid_from` | `DATETIME(3)` | NO | `None` |
| `valid_until` | `DATETIME(3)` | NO | `None` |
| `subtotal_amount` | `DECIMAL(18,2)` | NO | `None` |
| `discount_amount` | `DECIMAL(18,2)` | NO | `0` |
| `total_amount` | `DECIMAL(18,2)` | NO | `None` |
| `deposit_amount` | `DECIMAL(18,2)` | NO | `0` |
| `billing_cycle_months` | `INT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `approval_instance_id` | `VARCHAR(128)` | YES | `None` |
| `created_by` | `BIGINT` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_quote_ver` (tenant_id, quotation_id, version_no)

### `quotation_item`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `quotation_version_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `offering_id` | `BIGINT` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `quantity` | `DECIMAL(20,6)` | NO | `1` |
| `unit_price` | `DECIMAL(20,8)` | NO | `None` |
| `guidance_price` | `DECIMAL(18,2)` | YES | `None` |
| `floor_price` | `DECIMAL(18,2)` | YES | `None` |
| `discount_amount` | `DECIMAL(18,2)` | NO | `0` |
| `final_amount` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_quote_item_res` (tenant_id, resource_unit_id)

### `crm_activity`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `opportunity_id` | `BIGINT` | YES | `None` |
| `activity_type` | `VARCHAR(32)` | NO | `None` |
| `content` | `TEXT` | YES | `None` |
| `operator_id` | `BIGINT` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |
| `next_follow_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_activity_customer` (tenant_id, customer_id, occurred_at)

### `crm_task`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `task_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | YES | `None` |
| `opportunity_id` | `BIGINT` | YES | `None` |
| `task_type` | `VARCHAR(32)` | NO | `None` |
| `title` | `VARCHAR(256)` | NO | `None` |
| `due_at` | `DATETIME(3)` | YES | `None` |
| `owner_user_id` | `BIGINT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_crm_task` (tenant_id, task_no)
- KEY `idx_task_owner` (tenant_id, owner_user_id, status, due_at)

### `customer_vehicle`

Migration: `03-database/flyway/crm/V2__customer_vehicle.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `plate_no_ciphertext` | `VARCHAR(512)` | NO | `None` |
| `plate_no_hash` | `VARCHAR(128)` | NO | `None` |
| `vehicle_type` | `VARCHAR(32)` | NO | `None` |
| `brand` | `VARCHAR(64)` | YES | `None` |
| `model` | `VARCHAR(64)` | YES | `None` |
| `color` | `VARCHAR(32)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_vehicle_customer` (tenant_id, customer_id, status)
- KEY `idx_vehicle_plate_hash` (tenant_id, plate_no_hash)

## finance

### `receivable`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `receivable_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `bill_id` | `BIGINT` | NO | `None` |
| `charge_type` | `VARCHAR(32)` | NO | `None` |
| `due_date` | `DATE` | NO | `None` |
| `original_amount` | `DECIMAL(18,2)` | NO | `None` |
| `adjustment_amount` | `DECIMAL(18,2)` | NO | `0` |
| `receivable_amount` | `DECIMAL(18,2)` | NO | `None` |
| `allocated_amount` | `DECIMAL(18,2)` | NO | `0` |
| `outstanding_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_recv` (tenant_id, receivable_no)
- UNIQUE KEY `uk_recv_bill_charge` (tenant_id, bill_id, charge_type)
- KEY `idx_recv_customer` (tenant_id, customer_id, status, due_date)
- KEY `idx_recv_ag` (tenant_id, agreement_id)

### `receivable_adjustment`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `receivable_id` | `BIGINT` | NO | `None` |
| `adjustment_type` | `VARCHAR(32)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `reason` | `VARCHAR(512)` | NO | `None` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_by` | `BIGINT` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_recv_adj` (tenant_id, receivable_id, status)

### `collection_record`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `collection_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `collection_type` | `VARCHAR(32)` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `allocated_amount` | `DECIMAL(18,2)` | NO | `0` |
| `unallocated_amount` | `DECIMAL(18,2)` | NO | `None` |
| `collection_date` | `DATETIME(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_collection_no` (tenant_id, collection_no)
- UNIQUE KEY `uk_collection_source` (tenant_id, source_type, source_id)
- KEY `idx_collection_customer` (tenant_id, customer_id, status)

### `payment_allocation`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `collection_id` | `BIGINT` | NO | `None` |
| `receivable_id` | `BIGINT` | NO | `None` |
| `allocation_amount` | `DECIMAL(18,2)` | NO | `None` |
| `allocation_type` | `VARCHAR(32)` | NO | `None` |
| `operator_id` | `BIGINT` | YES | `None` |
| `allocated_at` | `DATETIME(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_alloc_collection` (tenant_id, collection_id)
- KEY `idx_alloc_recv` (tenant_id, receivable_id)

### `allocation_reversal`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `original_allocation_id` | `BIGINT` | NO | `None` |
| `reversal_amount` | `DECIMAL(18,2)` | NO | `None` |
| `reason` | `VARCHAR(512)` | NO | `None` |
| `operator_id` | `BIGINT` | NO | `None` |
| `reversed_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_alloc_rev_once` (tenant_id, original_allocation_id, id)

### `customer_advance`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `collection_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `original_amount` | `DECIMAL(18,2)` | NO | `None` |
| `available_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_adv_customer` (tenant_id, customer_id, status)

### `invoice_quota_reservation`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `allocation_id` | `BIGINT` | NO | `None` |
| `invoice_application_id` | `BIGINT` | NO | `None` |
| `reserved_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_quota_app` (tenant_id, invoice_application_id)
- KEY `idx_quota_alloc` (tenant_id, allocation_id, status)

### `account`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `account_code` | `VARCHAR(64)` | NO | `None` |
| `account_name` | `VARCHAR(128)` | NO | `None` |
| `account_type` | `VARCHAR(32)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_account` (tenant_id, account_code)

### `accounting_entry`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `entry_no` | `VARCHAR(64)` | NO | `None` |
| `business_type` | `VARCHAR(32)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `posting_type` | `VARCHAR(32)` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |
| `posting_date` | `DATE` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `description` | `VARCHAR(512)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_entry_no` (tenant_id, entry_no)
- UNIQUE KEY `uk_entry_biz` (tenant_id, business_type, business_id, posting_type)

### `accounting_line`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `entry_id` | `BIGINT` | NO | `None` |
| `account_code` | `VARCHAR(64)` | NO | `None` |
| `direction` | `VARCHAR(8)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `customer_id` | `BIGINT` | YES | `None` |
| `agreement_id` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_line_entry` (tenant_id, entry_id)

### `dunning_case`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `receivable_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `dunning_level` | `INT` | NO | `1` |
| `promised_amount` | `DECIMAL(18,2)` | YES | `None` |
| `promised_date` | `DATE` | YES | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `next_action_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_dunning_recv` (tenant_id, receivable_id)
- KEY `idx_dunning_owner` (tenant_id, owner_user_id, status, next_action_at)

### `reconciliation_batch`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `batch_no` | `VARCHAR(64)` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `statement_date` | `DATE` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `total_channel_count` | `BIGINT` | NO | `0` |
| `total_local_count` | `BIGINT` | NO | `0` |
| `matched_count` | `BIGINT` | NO | `0` |
| `exception_count` | `BIGINT` | NO | `0` |
| `started_at` | `DATETIME(3)` | YES | `None` |
| `finished_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_recon_batch` (tenant_id, batch_no)
- UNIQUE KEY `uk_recon_day` (tenant_id, channel, statement_date)

### `reconciliation_item`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `batch_id` | `BIGINT` | NO | `None` |
| `channel_trade_no` | `VARCHAR(128)` | YES | `None` |
| `payment_no` | `VARCHAR(64)` | YES | `None` |
| `channel_amount` | `DECIMAL(18,2)` | YES | `None` |
| `local_amount` | `DECIMAL(18,2)` | YES | `None` |
| `channel_status` | `VARCHAR(32)` | YES | `None` |
| `local_status` | `VARCHAR(32)` | YES | `None` |
| `result_type` | `VARCHAR(64)` | NO | `None` |
| `exception_id` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_recon_item_batch` (tenant_id, batch_id, result_type)

### `reconciliation_exception`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `exception_no` | `VARCHAR(64)` | NO | `None` |
| `exception_type` | `VARCHAR(64)` | NO | `None` |
| `severity` | `VARCHAR(16)` | NO | `None` |
| `business_type` | `VARCHAR(32)` | YES | `None` |
| `business_id` | `BIGINT` | YES | `None` |
| `channel_payload` | `JSON` | YES | `None` |
| `local_payload` | `JSON` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `resolution_type` | `VARCHAR(64)` | YES | `None` |
| `resolution_reason` | `VARCHAR(1024)` | YES | `None` |
| `resolved_by` | `BIGINT` | YES | `None` |
| `resolved_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_recon_ex` (tenant_id, exception_no)
- KEY `idx_recon_ex_status` (tenant_id, status, severity)

### `channel_statement`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `statement_date` | `DATE` | NO | `None` |
| `file_id` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_stmt` (tenant_id, channel, statement_date)

### `channel_statement_item`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `statement_id` | `BIGINT` | NO | `None` |
| `channel_trade_no` | `VARCHAR(128)` | NO | `None` |
| `merchant_order_no` | `VARCHAR(128)` | YES | `None` |
| `trade_type` | `VARCHAR(32)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_stmt_trade` (tenant_id, channel_trade_no)

### `refund_amount_reservation`

Migration: `03-database/flyway/finance/V2__refund_amount_reservation.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `reservation_no` | `VARCHAR(64)` | NO | `None` |
| `refund_request_id` | `VARCHAR(128)` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `original_payment_transaction_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `requested_amount` | `DECIMAL(18,2)` | NO | `None` |
| `reserved_amount` | `DECIMAL(18,2)` | NO | `None` |
| `confirmed_amount` | `DECIMAL(18,2)` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `expire_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_refund_reservation_no` (tenant_id, reservation_no)
- UNIQUE KEY `uk_refund_request` (tenant_id, refund_request_id)
- KEY `idx_refund_payment` (tenant_id, payment_order_id, status)
- KEY `idx_refund_reserved_status` (tenant_id, status, created_at)

### `refund_reservation_target`

Migration: `03-database/flyway/finance/V2__refund_amount_reservation.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `refund_reservation_id` | `BIGINT` | NO | `None` |
| `target_type` | `VARCHAR(32)` | NO | `None` |
| `target_id` | `BIGINT` | NO | `None` |
| `reserved_amount` | `DECIMAL(18,2)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_refund_target` (tenant_id, refund_reservation_id, target_type, target_id)
- KEY `idx_refund_target_source` (tenant_id, target_type, target_id)

### `security_deposit_account`

Migration: `03-database/flyway/finance/V3__security_deposit_unidentified_collection.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `agreement_id` | `BIGINT` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `required_amount` | `DECIMAL(18,2)` | NO | `None` |
| `received_amount` | `DECIMAL(18,2)` | NO | `0` |
| `deducted_amount` | `DECIMAL(18,2)` | NO | `0` |
| `refunded_amount` | `DECIMAL(18,2)` | NO | `0` |
| `reserved_refund_amount` | `DECIMAL(18,2)` | NO | `0` |
| `available_balance` | `DECIMAL(18,2)` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_security_deposit_agreement` (tenant_id, agreement_id)
- KEY `idx_security_deposit_customer` (tenant_id, customer_id, status)

### `security_deposit_transaction`

Migration: `03-database/flyway/finance/V3__security_deposit_unidentified_collection.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `deposit_account_id` | `BIGINT` | NO | `None` |
| `transaction_type` | `VARCHAR(32)` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | YES | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `idempotency_key` | `VARCHAR(128)` | YES | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_deposit_tx_idem` (tenant_id, deposit_account_id, idempotency_key)
- KEY `idx_deposit_tx_account` (tenant_id, deposit_account_id, occurred_at)

### `unidentified_collection`

Migration: `03-database/flyway/finance/V3__security_deposit_unidentified_collection.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | NO | `None` |
| `bank_account_id` | `BIGINT` | YES | `None` |
| `provider_reference` | `VARCHAR(128)` | YES | `None` |
| `payer_name_raw` | `VARCHAR(256)` | YES | `None` |
| `payer_account_masked` | `VARCHAR(128)` | YES | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `received_at` | `DATETIME(3)` | NO | `None` |
| `memo_raw` | `VARCHAR(1024)` | YES | `None` |
| `normalized_memo` | `VARCHAR(1024)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `claimed_customer_id` | `BIGINT` | YES | `None` |
| `claimed_agreement_id` | `BIGINT` | YES | `None` |
| `resulting_collection_id` | `BIGINT` | YES | `None` |
| `claimed_by` | `BIGINT` | YES | `None` |
| `claimed_at` | `DATETIME(3)` | YES | `None` |
| `claim_reason` | `VARCHAR(512)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_unidentified_source` (tenant_id, source_type, source_id)
- KEY `idx_unidentified_status` (tenant_id, status, received_at)

## iam

### `platform_user`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `username` | `VARCHAR(128)` | NO | `None` |
| `password_hash` | `VARCHAR(255)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_username` (username)

### `tenant_user_membership`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `user_id` | `BIGINT` | NO | `None` |
| `employee_no` | `VARCHAR(64)` | YES | `None` |
| `display_name` | `VARCHAR(128)` | YES | `None` |
| `primary_org_id` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_membership` (tenant_id, user_id)
- KEY `idx_membership_org` (tenant_id, primary_org_id, status)

### `role`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `role_code` | `VARCHAR(64)` | NO | `None` |
| `role_name` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `risk_level` | `INT` | NO | `1` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_role` (tenant_id, role_code)

### `permission`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `permission_code` | `VARCHAR(128)` | NO | `None` |
| `permission_name` | `VARCHAR(128)` | NO | `None` |
| `module_code` | `VARCHAR(64)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_perm` (permission_code)

### `role_permission`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `role_id` | `BIGINT` | NO | `None` |
| `permission_id` | `BIGINT` | NO | `None` |

Indexes:
- PRIMARY KEY `` (role_id, permission_id)

### `user_role`

Migration: `03-database/flyway/iam/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |
| `membership_id` | `BIGINT` | NO | `None` |
| `role_id` | `BIGINT` | NO | `None` |

Indexes:
- PRIMARY KEY `` (tenant_id, membership_id, role_id)

## integration

### `mq_outbox`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `event_id` | `VARCHAR(64)` | NO | `None` |
| `aggregate_type` | `VARCHAR(64)` | NO | `None` |
| `aggregate_id` | `VARCHAR(64)` | NO | `None` |
| `aggregate_version` | `BIGINT` | NO | `None` |
| `event_type` | `VARCHAR(128)` | NO | `None` |
| `event_version` | `VARCHAR(16)` | NO | `None` |
| `payload` | `JSON` | NO | `None` |
| `status` | `VARCHAR(16)` | NO | `None` |
| `retry_count` | `INT` | NO | `0` |
| `next_retry_at` | `DATETIME(3)` | YES | `None` |
| `claim_token` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `published_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_outbox_event` (event_id)
- KEY `idx_outbox_poll` (status, next_retry_at, id)

### `mq_inbox`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `consumer_group` | `VARCHAR(128)` | NO | `None` |
| `event_id` | `VARCHAR(64)` | NO | `None` |
| `aggregate_version` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(16)` | NO | `None` |
| `processed_at` | `DATETIME(3)` | YES | `None` |
| `error_message` | `VARCHAR(1024)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_inbox` (tenant_id, consumer_group, event_id)

### `integration_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `task_type` | `VARCHAR(64)` | NO | `None` |
| `business_type` | `VARCHAR(32)` | YES | `None` |
| `business_id` | `BIGINT` | YES | `None` |
| `event_id` | `VARCHAR(64)` | YES | `None` |
| `trace_id` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `retry_count` | `INT` | NO | `0` |
| `next_retry_at` | `DATETIME(3)` | YES | `None` |
| `last_error_code` | `VARCHAR(128)` | YES | `None` |
| `last_error_message` | `TEXT` | YES | `None` |
| `owner_user_id` | `BIGINT` | YES | `None` |
| `manual_required` | `TINYINT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_int_task` (tenant_id, status, next_retry_at)

### `idempotency_record`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `user_id` | `BIGINT` | YES | `None` |
| `api_code` | `VARCHAR(128)` | NO | `None` |
| `idempotency_key` | `VARCHAR(128)` | NO | `None` |
| `request_hash` | `VARCHAR(128)` | NO | `None` |
| `response_code` | `VARCHAR(128)` | YES | `None` |
| `response_body` | `JSON` | YES | `None` |
| `status` | `VARCHAR(16)` | NO | `None` |
| `expire_at` | `DATETIME(3)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_idem` (tenant_id, user_id, api_code, idempotency_key)

### `audit_log`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `operator_id` | `BIGINT` | YES | `None` |
| `support_session_id` | `BIGINT` | YES | `None` |
| `business_type` | `VARCHAR(32)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `action` | `VARCHAR(128)` | NO | `None` |
| `before_snapshot` | `JSON` | YES | `None` |
| `after_snapshot` | `JSON` | YES | `None` |
| `reason` | `VARCHAR(1024)` | YES | `None` |
| `ip` | `VARCHAR(64)` | YES | `None` |
| `device` | `VARCHAR(256)` | YES | `None` |
| `trace_id` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_audit_biz` (tenant_id, business_type, business_id, created_at)

### `export_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `task_no` | `VARCHAR(64)` | NO | `None` |
| `export_type` | `VARCHAR(64)` | NO | `None` |
| `request_json` | `JSON` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `file_id` | `BIGINT` | YES | `None` |
| `created_by` | `BIGINT` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `completed_at` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_export_task` (tenant_id, task_no)

### `import_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `task_no` | `VARCHAR(64)` | NO | `None` |
| `import_type` | `VARCHAR(64)` | NO | `None` |
| `source_file_id` | `BIGINT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `total_rows` | `BIGINT` | NO | `0` |
| `success_rows` | `BIGINT` | NO | `0` |
| `failed_rows` | `BIGINT` | NO | `0` |
| `created_by` | `BIGINT` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_import_task` (tenant_id, task_no)

## invoice

### `invoice_application`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `application_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `invoice_type` | `VARCHAR(32)` | NO | `None` |
| `title` | `VARCHAR(256)` | NO | `None` |
| `tax_no_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `bank_name` | `VARCHAR(256)` | YES | `None` |
| `bank_account_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `registered_address` | `VARCHAR(512)` | YES | `None` |
| `registered_phone_ciphertext` | `VARCHAR(512)` | YES | `None` |
| `requested_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `submitted_by` | `BIGINT` | YES | `None` |
| `submitted_at` | `DATETIME(3)` | YES | `None` |
| `approved_by` | `BIGINT` | YES | `None` |
| `approved_at` | `DATETIME(3)` | YES | `None` |
| `provider_request_no` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_inv_app` (tenant_id, application_no)
- UNIQUE KEY `uk_inv_provider_req` (provider_request_no)

### `invoice_application_item`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `application_id` | `BIGINT` | NO | `None` |
| `allocation_id` | `BIGINT` | NO | `None` |
| `requested_amount` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_invapp_alloc` (tenant_id, allocation_id)

### `invoice`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `invoice_no` | `VARCHAR(128)` | NO | `None` |
| `application_id` | `BIGINT` | NO | `None` |
| `provider` | `VARCHAR(32)` | NO | `None` |
| `provider_invoice_id` | `VARCHAR(128)` | YES | `None` |
| `invoice_type` | `VARCHAR(32)` | NO | `None` |
| `blue_or_red` | `VARCHAR(8)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `tax_amount` | `DECIMAL(18,2)` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `issued_at` | `DATETIME(3)` | YES | `None` |
| `pdf_file_id` | `BIGINT` | YES | `None` |
| `ofd_file_id` | `BIGINT` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `updated_by` | `BIGINT` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_invoice_no` (tenant_id, invoice_no)
- UNIQUE KEY `uk_provider_invoice` (provider, provider_invoice_id)

### `invoice_item`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `invoice_id` | `BIGINT` | NO | `None` |
| `item_name` | `VARCHAR(256)` | NO | `None` |
| `tax_rate` | `DECIMAL(20,8)` | YES | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `tax_amount` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_invoice_item` (tenant_id, invoice_id)

### `invoice_relation`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `source_invoice_id` | `BIGINT` | NO | `None` |
| `target_invoice_id` | `BIGINT` | NO | `None` |
| `relation_type` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_inv_relation` (tenant_id, source_invoice_id, target_invoice_id, relation_type)

### `invoice_red_flush_application`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `application_no` | `VARCHAR(64)` | NO | `None` |
| `original_invoice_id` | `BIGINT` | NO | `None` |
| `reason` | `VARCHAR(512)` | NO | `None` |
| `requested_amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `workflow_instance_id` | `VARCHAR(128)` | YES | `None` |
| `provider_request_no` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_red_app` (tenant_id, application_no)
- UNIQUE KEY `uk_red_provider_req` (provider_request_no)

### `invoice_delivery_instruction`

Migration: `03-database/flyway/invoice/V2__invoice_email_delivery.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `invoice_id` | `BIGINT` | NO | `None` |
| `application_id` | `BIGINT` | NO | `None` |
| `delivery_type` | `VARCHAR(32)` | NO | `None` |
| `source` | `VARCHAR(32)` | NO | `None` |
| `parent_instruction_id` | `BIGINT` | YES | `None` |
| `template_code` | `VARCHAR(64)` | NO | `None` |
| `subject_snapshot` | `VARCHAR(512)` | YES | `None` |
| `recipient_set_hash` | `VARCHAR(128)` | NO | `None` |
| `dedup_key` | `VARCHAR(256)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `notification_message_id` | `BIGINT` | YES | `None` |
| `requested_by` | `BIGINT` | YES | `None` |
| `requested_at` | `DATETIME(3)` | NO | `None` |
| `sent_at` | `DATETIME(3)` | YES | `None` |
| `failure_code` | `VARCHAR(128)` | YES | `None` |
| `failure_message` | `VARCHAR(512)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_invoice_delivery_invoice` (tenant_id, invoice_id, requested_at)
- KEY `idx_invoice_delivery_status` (tenant_id, status, requested_at)
- UNIQUE KEY `uk_invoice_delivery_dedup` (tenant_id, dedup_key)

### `invoice_delivery_recipient`

Migration: `03-database/flyway/invoice/V2__invoice_email_delivery.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `instruction_id` | `BIGINT` | NO | `None` |
| `recipient_type` | `VARCHAR(16)` | NO | `None` |
| `email_ciphertext` | `VARCHAR(1024)` | NO | `None` |
| `email_hash` | `VARCHAR(128)` | NO | `None` |
| `display_name` | `VARCHAR(256)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_invoice_delivery_recipient` (tenant_id, instruction_id)
- KEY `idx_invoice_delivery_email_hash` (tenant_id, email_hash)

## notification

### `notification_rule`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `rule_code` | `VARCHAR(64)` | NO | `None` |
| `business_event_type` | `VARCHAR(128)` | NO | `None` |
| `category` | `VARCHAR(32)` | NO | `None` |
| `recipient_strategy` | `VARCHAR(64)` | NO | `None` |
| `channel_json` | `JSON` | NO | `None` |
| `template_mapping_json` | `JSON` | NO | `None` |
| `fallback_policy` | `VARCHAR(32)` | NO | `'NONE'` |
| `quiet_hour_policy` | `VARCHAR(32)` | NO | `'DEFAULT'` |
| `priority` | `INT` | NO | `100` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_rule` (tenant_id, rule_code)
- KEY `idx_notification_rule_event` (tenant_id, business_event_type, status)

### `notification_template`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `template_code` | `VARCHAR(64)` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `category` | `VARCHAR(32)` | NO | `None` |
| `version_no` | `INT` | NO | `None` |
| `subject_template` | `VARCHAR(512)` | YES | `None` |
| `body_template` | `MEDIUMTEXT` | NO | `None` |
| `plain_text_template` | `MEDIUMTEXT` | YES | `None` |
| `provider_template_ref` | `VARCHAR(256)` | YES | `None` |
| `variable_schema_json` | `JSON` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_template` (tenant_id, template_code, channel, version_no)
- KEY `idx_notification_template_active` (tenant_id, template_code, channel, status)

### `notification_message`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `message_no` | `VARCHAR(64)` | NO | `None` |
| `category` | `VARCHAR(32)` | NO | `None` |
| `business_type` | `VARCHAR(64)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `business_event_id` | `VARCHAR(64)` | YES | `None` |
| `rule_code` | `VARCHAR(64)` | NO | `None` |
| `trigger_key` | `VARCHAR(128)` | NO | `None` |
| `recipient_ref_type` | `VARCHAR(32)` | NO | `None` |
| `recipient_ref_id` | `BIGINT` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `priority` | `INT` | NO | `100` |
| `scheduled_at` | `DATETIME(3)` | YES | `None` |
| `completed_at` | `DATETIME(3)` | YES | `None` |
| `dedup_key` | `VARCHAR(256)` | NO | `None` |
| `trace_id` | `VARCHAR(64)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_message_dedup` (tenant_id, dedup_key)
- UNIQUE KEY `uk_notification_message_no` (tenant_id, message_no)
- KEY `idx_notification_message_status` (tenant_id, status, scheduled_at)

### `notification_delivery`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `notification_message_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `template_code` | `VARCHAR(64)` | NO | `None` |
| `template_version` | `INT` | NO | `None` |
| `recipient_type` | `VARCHAR(16)` | NO | `'TO'` |
| `recipient_address_ciphertext` | `VARCHAR(1024)` | NO | `None` |
| `recipient_address_hash` | `VARCHAR(128)` | NO | `None` |
| `provider_code` | `VARCHAR(64)` | YES | `None` |
| `provider_message_id` | `VARCHAR(256)` | YES | `None` |
| `provider_request_no` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `result_uncertain` | `TINYINT` | NO | `0` |
| `retry_count` | `INT` | NO | `0` |
| `next_retry_at` | `DATETIME(3)` | YES | `None` |
| `content_hash` | `VARCHAR(128)` | YES | `None` |
| `last_error_code` | `VARCHAR(128)` | YES | `None` |
| `last_error_message` | `VARCHAR(512)` | YES | `None` |
| `sent_at` | `DATETIME(3)` | YES | `None` |
| `delivered_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_delivery_req` (provider_request_no)
- UNIQUE KEY `uk_notification_delivery_dedup` (tenant_id, notification_message_id, channel, recipient_address_hash, template_version)
- KEY `idx_notification_delivery_retry` (tenant_id, status, next_retry_at)
- KEY `idx_notification_delivery_provider` (tenant_id, provider_code, provider_message_id)

### `notification_delivery_attempt`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `delivery_id` | `BIGINT` | NO | `None` |
| `attempt_no` | `INT` | NO | `None` |
| `provider_request_no` | `VARCHAR(128)` | NO | `None` |
| `result_type` | `VARCHAR(32)` | NO | `None` |
| `provider_http_status` | `INT` | YES | `None` |
| `provider_error_code` | `VARCHAR(128)` | YES | `None` |
| `duration_ms` | `BIGINT` | YES | `None` |
| `attempted_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_attempt` (tenant_id, delivery_id, attempt_no)
- UNIQUE KEY `uk_notification_attempt_req` (provider_request_no)

### `notification_recipient_preference`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `subject_type` | `VARCHAR(32)` | NO | `None` |
| `subject_id` | `BIGINT` | NO | `None` |
| `marketing_email_enabled` | `TINYINT` | NO | `1` |
| `marketing_sms_enabled` | `TINYINT` | NO | `1` |
| `transactional_email_enabled` | `TINYINT` | NO | `1` |
| `transactional_sms_enabled` | `TINYINT` | NO | `1` |
| `preferred_channel` | `VARCHAR(32)` | YES | `None` |
| `timezone` | `VARCHAR(64)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_pref` (tenant_id, subject_type, subject_id)

### `notification_provider_config`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `provider_mode` | `VARCHAR(32)` | NO | `None` |
| `provider_code` | `VARCHAR(64)` | NO | `None` |
| `sender_identity` | `VARCHAR(256)` | YES | `None` |
| `credential_ref` | `VARCHAR(512)` | NO | `None` |
| `callback_secret_ref` | `VARCHAR(512)` | YES | `None` |
| `config_json` | `JSON` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_notification_provider` (tenant_id, channel, provider_code)

### `notification_suppression`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `recipient_address_hash` | `VARCHAR(128)` | NO | `None` |
| `reason_type` | `VARCHAR(32)` | NO | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_notification_suppress` (tenant_id, channel, recipient_address_hash, effective_from, effective_to)

## organization

### `organization_unit`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `parent_id` | `BIGINT` | YES | `None` |
| `org_code` | `VARCHAR(64)` | NO | `None` |
| `org_name` | `VARCHAR(128)` | NO | `None` |
| `org_type` | `VARCHAR(32)` | NO | `None` |
| `path` | `VARCHAR(1024)` | NO | `None` |
| `level_no` | `INT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_org` (tenant_id, org_code)
- KEY `idx_org_parent` (tenant_id, parent_id)

### `management_team`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `team_code` | `VARCHAR(64)` | NO | `None` |
| `team_name` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_team` (tenant_id, team_code)

### `management_team_member`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `team_id` | `BIGINT` | NO | `None` |
| `membership_id` | `BIGINT` | NO | `None` |
| `role_code` | `VARCHAR(64)` | YES | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_team_member` (tenant_id, team_id, status)

### `team_resource_relation`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `team_id` | `BIGINT` | NO | `None` |
| `resource_type` | `VARCHAR(32)` | NO | `None` |
| `resource_id` | `BIGINT` | NO | `None` |
| `relation_type` | `VARCHAR(32)` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_team_res` (tenant_id, resource_type, resource_id)

### `resource_acl`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `principal_type` | `VARCHAR(32)` | NO | `None` |
| `principal_id` | `BIGINT` | NO | `None` |
| `resource_type` | `VARCHAR(32)` | NO | `None` |
| `resource_id` | `BIGINT` | NO | `None` |
| `permission_code` | `VARCHAR(128)` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_acl_resource` (tenant_id, resource_type, resource_id)

### `ownership_history`

Migration: `03-database/flyway/organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `business_type` | `VARCHAR(32)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `old_owner_id` | `BIGINT` | YES | `None` |
| `new_owner_id` | `BIGINT` | YES | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `changed_by` | `BIGINT` | NO | `None` |
| `changed_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_owner_hist` (tenant_id, business_type, business_id, changed_at)

## owner-settlement

### `property_owner`

Migration: `03-database/flyway/owner-settlement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `owner_no` | `VARCHAR(64)` | NO | `None` |
| `owner_type` | `VARCHAR(32)` | NO | `None` |
| `owner_name` | `VARCHAR(256)` | NO | `None` |
| `tax_no` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_property_owner_no` (tenant_id, owner_no)

### `owner_operating_agreement`

Migration: `03-database/flyway/owner-settlement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `owner_id` | `BIGINT` | NO | `None` |
| `asset_id` | `BIGINT` | NO | `None` |
| `agreement_no` | `VARCHAR(64)` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_owner_operating_agreement` (tenant_id, agreement_no)
- KEY `idx_owner_operating_asset` (tenant_id, asset_id, status)

### `owner_settlement_rule`

Migration: `03-database/flyway/owner-settlement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `owner_operating_agreement_id` | `BIGINT` | NO | `None` |
| `rule_type` | `VARCHAR(32)` | NO | `None` |
| `rule_json` | `JSON` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `version_no` | `INT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_owner_settlement_rule_version` (tenant_id, owner_operating_agreement_id, version_no)

### `owner_settlement_batch`

Migration: `03-database/flyway/owner-settlement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `batch_no` | `VARCHAR(64)` | NO | `None` |
| `owner_id` | `BIGINT` | NO | `None` |
| `period_start` | `DATE` | NO | `None` |
| `period_end` | `DATE` | NO | `None` |
| `gross_eligible_amount` | `DECIMAL(18,2)` | NO | `None` |
| `deduction_amount` | `DECIMAL(18,2)` | NO | `0` |
| `payable_amount` | `DECIMAL(18,2)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_owner_settlement_batch` (tenant_id, batch_no)
- KEY `idx_owner_settlement_owner` (tenant_id, owner_id, status, period_end)

### `owner_settlement_item`

Migration: `03-database/flyway/owner-settlement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `batch_id` | `BIGINT` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | NO | `None` |
| `gross_amount` | `DECIMAL(18,2)` | NO | `None` |
| `deduction_amount` | `DECIMAL(18,2)` | NO | `0` |
| `settlement_amount` | `DECIMAL(18,2)` | NO | `None` |
| `calculation_trace_json` | `JSON` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_owner_settlement_source` (tenant_id, batch_id, source_type, source_id)

## payment

### `payment_order`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payment_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `expire_at` | `DATETIME(3)` | YES | `None` |
| `provider_merchant_ref` | `VARCHAR(128)` | YES | `None` |
| `idempotency_key` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_payment_no` (payment_no)
- UNIQUE KEY `uk_payment_idem` (tenant_id, idempotency_key)
- KEY `idx_payment_customer` (tenant_id, customer_id, status)

### `payment_business_relation`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `business_type` | `VARCHAR(32)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `expected_amount` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_pay_biz` (tenant_id, business_type, business_id)

### `payment_transaction`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `channel_trade_no` | `VARCHAR(128)` | NO | `None` |
| `transaction_type` | `VARCHAR(32)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `currency` | `CHAR(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |
| `raw_payload_hash` | `VARCHAR(128)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_channel_trade` (channel, channel_trade_no)
- KEY `idx_pt_order` (tenant_id, payment_order_id)

### `payment_callback_log`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `0` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `request_id` | `VARCHAR(128)` | YES | `None` |
| `merchant_ref` | `VARCHAR(128)` | YES | `None` |
| `payment_no` | `VARCHAR(64)` | YES | `None` |
| `body_hash` | `VARCHAR(128)` | NO | `None` |
| `signature_valid` | `TINYINT` | NO | `None` |
| `process_result` | `VARCHAR(64)` | NO | `None` |
| `received_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_cb_payment` (payment_no, received_at)

### `refund_order`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `refund_no` | `VARCHAR(64)` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `original_transaction_id` | `BIGINT` | NO | `None` |
| `refund_amount` | `DECIMAL(18,2)` | NO | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `provider_refund_no` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_refund_no` (refund_no)
- KEY `idx_refund_payment` (tenant_id, payment_order_id, status)

### `refund_transaction`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `refund_order_id` | `BIGINT` | NO | `None` |
| `provider_refund_no` | `VARCHAR(128)` | NO | `None` |
| `amount` | `DECIMAL(18,2)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `occurred_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_provider_refund` (provider_refund_no)

### `tenant_payment_merchant`

Migration: `03-database/flyway/payment/V2__payment_domain_hardening.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `merchant_mode` | `VARCHAR(32)` | NO | `None` |
| `merchant_id` | `VARCHAR(128)` | NO | `None` |
| `app_id` | `VARCHAR(128)` | YES | `None` |
| `credential_ref` | `VARCHAR(512)` | NO | `None` |
| `certificate_ref` | `VARCHAR(512)` | YES | `None` |
| `callback_profile` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `priority` | `INT` | NO | `100` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_by` | `BIGINT` | YES | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tenant_merchant` (tenant_id, channel, merchant_id, app_id)
- KEY `idx_merchant_resolve` (channel, merchant_id, app_id, status)
- KEY `idx_tenant_channel_merchant` (tenant_id, channel, status, priority)

### `payment_attempt`

Migration: `03-database/flyway/payment/V2__payment_domain_hardening.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `attempt_no` | `VARCHAR(64)` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `payment_method` | `VARCHAR(64)` | NO | `None` |
| `merchant_config_id` | `BIGINT` | NO | `None` |
| `provider_request_no` | `VARCHAR(128)` | NO | `None` |
| `provider_prepay_id` | `VARCHAR(256)` | YES | `None` |
| `provider_status` | `VARCHAR(64)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `client_action_payload` | `JSON` | YES | `None` |
| `expire_at` | `DATETIME(3)` | YES | `None` |
| `unknown_since` | `DATETIME(3)` | YES | `None` |
| `last_query_at` | `DATETIME(3)` | YES | `None` |
| `query_count` | `INT` | NO | `0` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_payment_attempt_no` (attempt_no)
- UNIQUE KEY `uk_provider_request` (channel, merchant_config_id, provider_request_no)
- KEY `idx_attempt_order` (tenant_id, payment_order_id, status)
- KEY `idx_attempt_unknown` (tenant_id, status, unknown_since, id)

### `payment_channel_request`

Migration: `03-database/flyway/payment/V2__payment_domain_hardening.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `request_no` | `VARCHAR(64)` | NO | `None` |
| `operation_type` | `VARCHAR(32)` | NO | `None` |
| `business_type` | `VARCHAR(32)` | NO | `None` |
| `business_id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `merchant_config_id` | `BIGINT` | NO | `None` |
| `provider_request_id` | `VARCHAR(128)` | YES | `None` |
| `request_hash` | `VARCHAR(128)` | YES | `None` |
| `response_hash` | `VARCHAR(128)` | YES | `None` |
| `http_status` | `INT` | YES | `None` |
| `result_type` | `VARCHAR(32)` | NO | `None` |
| `provider_code` | `VARCHAR(128)` | YES | `None` |
| `provider_message` | `VARCHAR(512)` | YES | `None` |
| `duration_ms` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_channel_request_no` (request_no)
- KEY `idx_channel_request_biz` (tenant_id, business_type, business_id, created_at)
- KEY `idx_channel_request_provider` (channel, merchant_config_id, provider_request_id)

### `payment_order_state_log`

Migration: `03-database/flyway/payment/V2__payment_domain_hardening.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `payment_order_id` | `BIGINT` | NO | `None` |
| `from_status` | `VARCHAR(32)` | YES | `None` |
| `to_status` | `VARCHAR(32)` | NO | `None` |
| `status_source` | `VARCHAR(32)` | NO | `None` |
| `reason_code` | `VARCHAR(64)` | YES | `None` |
| `evidence_type` | `VARCHAR(32)` | YES | `None` |
| `evidence_id` | `VARCHAR(128)` | YES | `None` |
| `trace_id` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_payment_state_order` (tenant_id, payment_order_id, created_at)

### `refund_order_state_log`

Migration: `03-database/flyway/payment/V2__payment_domain_hardening.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `refund_order_id` | `BIGINT` | NO | `None` |
| `from_status` | `VARCHAR(32)` | YES | `None` |
| `to_status` | `VARCHAR(32)` | NO | `None` |
| `status_source` | `VARCHAR(32)` | NO | `None` |
| `reason_code` | `VARCHAR(64)` | YES | `None` |
| `evidence_type` | `VARCHAR(32)` | YES | `None` |
| `evidence_id` | `VARCHAR(128)` | YES | `None` |
| `trace_id` | `VARCHAR(128)` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_refund_state_order` (tenant_id, refund_order_id, created_at)

## reservation

### `resource_schedule_guard`

Migration: `03-database/flyway/reservation/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `schedule_version` | `BIGINT` | NO | `0` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (tenant_id, resource_unit_id)

### `resource_availability`

Migration: `03-database/flyway/reservation/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `reason_type` | `VARCHAR(32)` | YES | `None` |
| `reason` | `VARCHAR(512)` | YES | `None` |
| `source_type` | `VARCHAR(32)` | YES | `None` |
| `source_id` | `BIGINT` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_avail` (tenant_id, resource_unit_id, status, start_time, end_time)

### `resource_occupancy`

Migration: `03-database/flyway/reservation/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `occupancy_type` | `VARCHAR(32)` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `source_type` | `VARCHAR(32)` | NO | `None` |
| `source_id` | `BIGINT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_occ_time` (tenant_id, resource_unit_id, status, start_time, end_time)
- KEY `idx_occ_source` (tenant_id, source_type, source_id)

### `reservation`

Migration: `03-database/flyway/reservation/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `reservation_no` | `VARCHAR(64)` | NO | `None` |
| `customer_id` | `BIGINT` | NO | `None` |
| `opportunity_id` | `BIGINT` | YES | `None` |
| `quotation_version_id` | `BIGINT` | NO | `None` |
| `hold_expire_at` | `DATETIME(3)` | NO | `None` |
| `deposit_required` | `TINYINT` | NO | `0` |
| `deposit_amount` | `DECIMAL(18,2)` | NO | `0` |
| `deposit_paid_amount` | `DECIMAL(18,2)` | NO | `0` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `converted_agreement_id` | `BIGINT` | YES | `None` |
| `version` | `INT` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_resv` (tenant_id, reservation_no)
- KEY `idx_resv_expire` (tenant_id, status, hold_expire_at)
- KEY `idx_resv_customer` (tenant_id, customer_id)

### `reservation_item`

Migration: `03-database/flyway/reservation/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `reservation_id` | `BIGINT` | NO | `None` |
| `resource_unit_id` | `BIGINT` | NO | `None` |
| `offering_id` | `BIGINT` | NO | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `end_time` | `DATETIME(3)` | NO | `None` |
| `quoted_price` | `DECIMAL(18,2)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_resv_item` (tenant_id, resource_unit_id, start_time, end_time)

## tax

### `tax_category`

Migration: `03-database/flyway/tax/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `0` |
| `category_code` | `VARCHAR(64)` | NO | `None` |
| `category_name` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tax_category` (tenant_id, category_code)

### `tax_rule`

Migration: `03-database/flyway/tax/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `0` |
| `jurisdiction_code` | `VARCHAR(64)` | NO | `None` |
| `tax_category_code` | `VARCHAR(64)` | NO | `None` |
| `tax_mode` | `VARCHAR(32)` | NO | `None` |
| `tax_rate` | `DECIMAL(20,8)` | NO | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `version_no` | `INT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tax_rule_version` (tenant_id, jurisdiction_code, tax_category_code, version_no)
- KEY `idx_tax_rule_active` (tenant_id, jurisdiction_code, tax_category_code, status, effective_from, effective_to)

## tenant

### `tenant`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_code` | `VARCHAR(64)` | NO | `None` |
| `tenant_name` | `VARCHAR(128)` | NO | `None` |
| `tenant_type` | `VARCHAR(32)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `package_id` | `BIGINT` | YES | `None` |
| `isolation_mode` | `VARCHAR(32)` | NO | `'SHARED_DATABASE_SHARED_SCHEMA'` |
| `timezone` | `VARCHAR(64)` | NO | `'Asia/Shanghai'` |
| `currency` | `CHAR(3)` | NO | `'CNY'` |
| `locale` | `VARCHAR(32)` | NO | `'zh-CN'` |
| `expires_at` | `DATETIME(3)` | YES | `None` |
| `created_by` | `BIGINT` | YES | `None` |
| `created_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |
| `updated_by` | `BIGINT` | YES | `None` |
| `updated_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tenant_code` (tenant_code)
- KEY `idx_tenant_status` (status)

### `tenant_package`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `package_code` | `VARCHAR(64)` | NO | `None` |
| `package_name` | `VARCHAR(128)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `capability_json` | `JSON` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_pkg_code` (package_code)

### `tenant_quota`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `max_users` | `BIGINT` | NO | `100` |
| `max_assets` | `BIGINT` | NO | `1000` |
| `max_resources` | `BIGINT` | NO | `10000` |
| `max_storage_bytes` | `BIGINT` | NO | `10737418240` |
| `max_monthly_api_calls` | `BIGINT` | NO | `1000000` |
| `version` | `INT` | NO | `0` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_quota_tenant` (tenant_id)

### `tenant_config`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `config_key` | `VARCHAR(128)` | NO | `None` |
| `config_value` | `TEXT` | YES | `None` |
| `value_type` | `VARCHAR(32)` | NO | `'STRING'` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `version_no` | `INT` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tenant_cfg_ver` (tenant_id, config_key, version_no)
- KEY `idx_tenant_cfg_active` (tenant_id, config_key, status)

### `tenant_feature`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `feature_key` | `VARCHAR(128)` | NO | `None` |
| `enabled` | `TINYINT` | NO | `0` |
| `rollout_percentage` | `INT` | NO | `100` |
| `effective_from` | `DATETIME(3)` | YES | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_tenant_feature` (tenant_id, feature_key)

### `tenant_route`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |
| `route_type` | `VARCHAR(32)` | NO | `None` |
| `datasource_key` | `VARCHAR(128)` | NO | `None` |
| `schema_name` | `VARCHAR(128)` | YES | `None` |
| `region` | `VARCHAR(64)` | YES | `None` |
| `storage_namespace` | `VARCHAR(128)` | YES | `None` |
| `search_namespace` | `VARCHAR(128)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `route_version` | `BIGINT` | NO | `0` |
| `updated_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |

Indexes:
- PRIMARY KEY `` (tenant_id)

### `tenant_branding`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `brand_name` | `VARCHAR(128)` | YES | `None` |
| `logo_file_id` | `BIGINT` | YES | `None` |
| `theme_json` | `JSON` | YES | `None` |
| `custom_domain` | `VARCHAR(255)` | YES | `None` |

Indexes:
- PRIMARY KEY `` (id)
- UNIQUE KEY `uk_brand_tenant` (tenant_id)
- UNIQUE KEY `uk_brand_domain` (custom_domain)

### `support_session`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `platform_user_id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `reason` | `VARCHAR(512)` | NO | `None` |
| `permissions_json` | `JSON` | NO | `None` |
| `approved_by` | `BIGINT` | YES | `None` |
| `start_time` | `DATETIME(3)` | NO | `None` |
| `expire_time` | `DATETIME(3)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `CURRENT_TIMESTAMP(3)` |

Indexes:
- PRIMARY KEY `` (id)
- KEY `idx_support_tenant` (tenant_id, status, expire_time)
