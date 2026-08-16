# Database Data Dictionary — V7.0

Total parsed tables: **141**.

> Canonical executable schema remains the Flyway SQL. This document is generated for code review/codegen navigation.

## agreement

### `agreement`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `agreement_item`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `agreement_snapshot`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `agreement_change`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `agreement_sign_process`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `renewal_priority`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `handover_order`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `handover_item`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `signature_process`

Migration: `03-database/flyway/agreement/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `parking_vehicle_binding`

Migration: `03-database/flyway/agreement/V2__parking_vehicle_binding.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
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
| `id` | `BIGINT` | NO | `0` |

### `asset_space`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `resource_unit`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `resource_schedule_guard`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `0` |

### `resource_conflict_group`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `resource_conflict_group_member`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |

### `valuation`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `offering`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `listing`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `resource_availability`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `resource_occupancy`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `reservation`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `reservation_item`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `renovation_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `maintenance_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `operation_work_order`

Migration: `03-database/flyway/asset/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `parking_space_profile`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
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
- UNIQUE KEY `uk_parking_profile_resource` (tenant_id, resource_unit_id)
- UNIQUE KEY `uk_parking_no` (tenant_id, parking_no)
- KEY `idx_parking_zone` (tenant_id, zone, status)

### `utility_meter`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_no` | `VARCHAR(64)` | NO | `None` |
| `utility_type` | `VARCHAR(32)` | NO | `None` |
| `unit_code` | `VARCHAR(16)` | NO | `None` |
| `meter_scope` | `VARCHAR(32)` | NO | `None` |
| `reading_mode` | `VARCHAR(32)` | NO | `None` |
| `multiplier` | `DECIMAL(18,8)` | NO | `1` |
| `manufacturer` | `VARCHAR(128)` | YES | `None` |
| `serial_no` | `VARCHAR(128)` | YES | `None` |
| `installed_at` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- UNIQUE KEY `uk_utility_meter_no` (tenant_id, meter_no)
- KEY `idx_utility_meter_type` (tenant_id, utility_type, status)

### `utility_meter_binding`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_id` | `BIGINT` | NO | `None` |
| `asset_id` | `BIGINT` | YES | `None` |
| `space_id` | `BIGINT` | YES | `None` |
| `resource_unit_id` | `BIGINT` | YES | `None` |
| `allocation_method` | `VARCHAR(32)` | NO | `None` |
| `allocation_ratio` | `DECIMAL(18,8)` | YES | `None` |
| `effective_from` | `DATETIME(3)` | NO | `None` |
| `effective_to` | `DATETIME(3)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |
| `updated_at` | `DATETIME(3)` | NO | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- KEY `idx_meter_binding_meter` (tenant_id, meter_id, status, effective_from, effective_to)
- KEY `idx_meter_binding_resource` (tenant_id, resource_unit_id, status, effective_from, effective_to)

### `utility_meter_reading`

Migration: `03-database/flyway/asset/V2__utility_meter_and_parking.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `meter_id` | `BIGINT` | NO | `None` |
| `reading_no` | `VARCHAR(64)` | NO | `None` |
| `period_start` | `DATETIME(3)` | NO | `None` |
| `period_end` | `DATETIME(3)` | NO | `None` |
| `previous_reading` | `DECIMAL(20,8)` | NO | `None` |
| `current_reading` | `DECIMAL(20,8)` | NO | `None` |
| `consumption` | `DECIMAL(20,8)` | NO | `None` |
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
- UNIQUE KEY `uk_meter_reading_no` (tenant_id, reading_no)
- UNIQUE KEY `uk_meter_period_version` (tenant_id, meter_id, period_start, period_end, version_no)
- KEY `idx_meter_reading_period` (tenant_id, meter_id, status, period_start, period_end)
- KEY `idx_meter_reading_handover` (tenant_id, handover_order_id)

## billing

### `billing_rule`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `billing_plan`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `billing_plan_item`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `bill`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `bill_item`

Migration: `03-database/flyway/billing/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `utility_tariff_plan`

Migration: `03-database/flyway/billing/V2__utility_property_parking_billing.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
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
- UNIQUE KEY `uk_tariff_code_ver` (tenant_id, tariff_code, version_no)
- KEY `idx_tariff_active` (tenant_id, utility_type, status, effective_from, effective_to)

### `utility_tariff_tier`

Migration: `03-database/flyway/billing/V2__utility_property_parking_billing.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
| `tenant_id` | `BIGINT` | NO | `None` |
| `tariff_plan_id` | `BIGINT` | NO | `None` |
| `tier_no` | `INT` | NO | `None` |
| `threshold_from` | `DECIMAL(20,8)` | NO | `0` |
| `threshold_to` | `DECIMAL(20,8)` | YES | `None` |
| `time_bucket` | `VARCHAR(32)` | YES | `None` |
| `unit_price` | `DECIMAL(18,8)` | NO | `None` |

Indexes:
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
| `id` | `BIGINT` | NO | `0` |

### `customer`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `customer_contact`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `customer_merge_record`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `customer_ownership_history`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `opportunity`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `opportunity_stage_history`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `viewing_appointment`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `viewing_resource_relation`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |

### `quotation`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `quotation_version`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `quotation_item`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `crm_activity`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `crm_task`

Migration: `03-database/flyway/crm/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `customer_vehicle`

Migration: `03-database/flyway/crm/V2__customer_vehicle.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | YES | `None` |
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
- KEY `idx_vehicle_customer` (tenant_id, customer_id, status)
- KEY `idx_vehicle_plate_hash` (tenant_id, plate_no_hash)

## finance

### `receivable`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `receivable_adjustment`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `collection_record`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `payment_allocation`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `allocation_reversal`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `customer_advance`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `invoice_quota_reservation`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `account`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `accounting_entry`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `accounting_line`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `dunning_case`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `reconciliation_batch`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `reconciliation_item`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `reconciliation_exception`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `channel_statement`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `channel_statement_item`

Migration: `03-database/flyway/finance/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

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

## iam-organization

### `platform_user`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `CURRENT_TIMESTAMP(3)` |

### `tenant_user_membership`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `CURRENT_TIMESTAMP(3)` |

### `role`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `1` |

### `permission`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `role_permission`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `role_id` | `BIGINT` | NO | `None` |

### `user_role`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `None` |

### `organization_unit`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `management_team`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `management_team_member`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `team_resource_relation`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `resource_acl`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `ownership_history`

Migration: `03-database/flyway/iam-organization/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

## integration

### `mq_outbox`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `mq_inbox`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `integration_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `idempotency_record`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `audit_log`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `export_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `import_task`

Migration: `03-database/flyway/integration/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

## invoice

### `invoice_application`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `invoice_application_item`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `invoice`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `invoice_item`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `invoice_relation`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `invoice_red_flush_application`

Migration: `03-database/flyway/invoice/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `invoice_delivery_instruction`

Migration: `03-database/flyway/invoice/V2__invoice_email_delivery.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `application_id` | `BIGINT` | NO | `None` |
| `source` | `VARCHAR(32)` | NO | `None` |
| `template_code` | `VARCHAR(64)` | NO | `None` |
| `recipient_set_hash` | `VARCHAR(128)` | NO | `None` |
| `notification_message_id` | `BIGINT` | YES | `None` |
| `requested_at` | `DATETIME(3)` | NO | `None` |
| `failure_code` | `VARCHAR(128)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- KEY `idx_invoice_delivery_invoice` (tenant_id, invoice_id, requested_at)
- KEY `idx_invoice_delivery_status` (tenant_id, status, requested_at)
- UNIQUE KEY `uk_invoice_delivery_dedup` (tenant_id, dedup_key)

### `invoice_delivery_recipient`

Migration: `03-database/flyway/invoice/V2__invoice_email_delivery.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `recipient_type` | `VARCHAR(16)` | NO | `None` |
| `email_hash` | `VARCHAR(128)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- KEY `idx_invoice_delivery_recipient` (tenant_id, instruction_id)
- KEY `idx_invoice_delivery_email_hash` (tenant_id, email_hash)

## notification

### `notification_rule`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `business_event_type` | `VARCHAR(128)` | NO | `None` |
| `recipient_strategy` | `VARCHAR(64)` | NO | `None` |
| `template_mapping_json` | `JSON` | NO | `'NONE'` |
| `quiet_hour_policy` | `VARCHAR(32)` | NO | `'DEFAULT'` |
| `status` | `VARCHAR(32)` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- UNIQUE KEY `uk_notification_rule` (tenant_id, rule_code)
- KEY `idx_notification_rule_event` (tenant_id, business_event_type, status)

### `notification_template`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `subject_template` | `VARCHAR(512)` | NO | `None` |
| `plain_text_template` | `MEDIUMTEXT` | YES | `None` |
| `variable_schema_json` | `JSON` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- UNIQUE KEY `uk_notification_template` (tenant_id, template_code, channel, version_no)
- KEY `idx_notification_template_active` (tenant_id, template_code, channel, status)

### `notification_message`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `category` | `VARCHAR(32)` | NO | `None` |
| `business_event_id` | `VARCHAR(64)` | NO | `None` |
| `recipient_ref_type` | `VARCHAR(32)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `100` |
| `scheduled_at` | `DATETIME(3)` | YES | `None` |
| `dedup_key` | `VARCHAR(256)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- UNIQUE KEY `uk_notification_message_dedup` (tenant_id, dedup_key)
- UNIQUE KEY `uk_notification_message_no` (tenant_id, message_no)
- KEY `idx_notification_message_status` (tenant_id, status, scheduled_at)

### `notification_delivery`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `channel` | `VARCHAR(32)` | NO | `None` |
| `recipient_type` | `VARCHAR(16)` | NO | `'TO'` |
| `recipient_address_hash` | `VARCHAR(128)` | NO | `None` |
| `provider_message_id` | `VARCHAR(256)` | NO | `None` |
| `status` | `VARCHAR(32)` | NO | `0` |
| `retry_count` | `INT` | NO | `0` |
| `content_hash` | `VARCHAR(128)` | YES | `None` |
| `sent_at` | `DATETIME(3)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- UNIQUE KEY `uk_notification_delivery_req` (provider_request_no)
- UNIQUE KEY `uk_notification_delivery_dedup` (tenant_id, notification_message_id, channel, recipient_address_hash, template_version)
- KEY `idx_notification_delivery_retry` (tenant_id, status, next_retry_at)
- KEY `idx_notification_delivery_provider` (tenant_id, provider_code, provider_message_id)

### `notification_delivery_attempt`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `attempt_no` | `INT` | NO | `None` |
| `result_type` | `VARCHAR(32)` | NO | `None` |
| `provider_error_code` | `VARCHAR(128)` | YES | `None` |
| `attempted_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- UNIQUE KEY `uk_notification_attempt` (tenant_id, delivery_id, attempt_no)
- UNIQUE KEY `uk_notification_attempt_req` (provider_request_no)

### `notification_recipient_preference`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `marketing_email_enabled` | `TINYINT` | NO | `1` |
| `transactional_email_enabled` | `TINYINT` | NO | `1` |
| `preferred_channel` | `VARCHAR(32)` | YES | `None` |
| `version` | `INT` | NO | `0` |

Indexes:
- UNIQUE KEY `uk_notification_pref` (tenant_id, subject_type, subject_id)

### `notification_provider_config`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `provider_mode` | `VARCHAR(32)` | NO | `None` |
| `sender_identity` | `VARCHAR(256)` | NO | `None` |
| `callback_secret_ref` | `VARCHAR(512)` | YES | `None` |
| `status` | `VARCHAR(32)` | NO | `0` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- UNIQUE KEY `uk_notification_provider` (tenant_id, channel, provider_code)

### `notification_suppression`

Migration: `03-database/flyway/notification/V1__init_notification.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `recipient_address_hash` | `VARCHAR(128)` | NO | `None` |
| `reason` | `VARCHAR(512)` | NO | `None` |
| `created_at` | `DATETIME(3)` | NO | `None` |

Indexes:
- KEY `idx_notification_suppress` (tenant_id, channel, recipient_address_hash, effective_from, effective_to)

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
| `id` | `BIGINT` | NO | `0` |

### `payment_business_relation`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `payment_transaction`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `payment_callback_log`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `refund_order`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `refund_transaction`

Migration: `03-database/flyway/payment/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

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

## tax

### `tax_category`

Migration: `03-database/flyway/tax/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |
| `tenant_id` | `BIGINT` | YES | `None` |
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
| `tenant_id` | `BIGINT` | YES | `None` |
| `jurisdiction_code` | `VARCHAR(64)` | NO | `None` |
| `tax_category_code` | `VARCHAR(64)` | NO | `None` |
| `tax_mode` | `VARCHAR(32)` | NO | `None` |
| `tax_rate` | `DECIMAL(12,8)` | NO | `None` |
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
| `tenant_type` | `VARCHAR(32)` | NO | `None` |
| `isolation_mode` | `VARCHAR(32)` | NO | `'SHARED_DATABASE_SHARED_SCHEMA'` |
| `currency` | `CHAR(3)` | NO | `'CNY'` |
| `created_by` | `BIGINT` | NO | `CURRENT_TIMESTAMP(3)` |

Indexes:
- UNIQUE KEY `uk_tenant_code` (tenant_code), KEY idx_tenant_status(status)

### `tenant_package`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `tenant_quota`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `100` |

### `tenant_config`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `'STRING'` |

### `tenant_feature`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `0` |

### `tenant_route`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `tenant_id` | `BIGINT` | NO | `0` |

### `tenant_branding`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `None` |

### `support_session`

Migration: `03-database/flyway/tenant/V1__init.sql`

| Column | Type | Nullable | Default |
|---|---|---:|---|
| `id` | `BIGINT` | NO | `CURRENT_TIMESTAMP(3)` |
