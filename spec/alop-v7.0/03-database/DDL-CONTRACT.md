# DDL Contract — V7.0

## Required rules
- Business tables are tenant-scoped unless explicitly platform-global.
- Tenant-scoped unique keys include `tenant_id` unless the business number is intentionally globally unique (e.g. provider-facing payment numbers may additionally have a global unique constraint).
- High-risk IDs and references are BIGINT.
- Money is DECIMAL(18,2); utility quantity/rates use the precision specified in the domain migration. See *Money & Rounding Policy* below for the authoritative precision matrix.
- Financial fact rows are not physically deleted.
- Every hot tenant query must have a tenant-leading usable index.
- Schedule-changing writes lock `resource_schedule_guard` first in ascending affected resource order.
- Flyway is the only production schema mutation path.
- Zero-downtime change: Expand -> Migrate -> Contract.

## Money & Rounding Policy

Authoritative numeric precision baseline — references `MASTER-SPEC-V7.0.md §5`. All DDL in `03-database/flyway/` MUST conform.

| Domain concept | SQL type | Notes |
|---|---|---|
| Money / 金额 | `DECIMAL(18,2)` | All monetary amounts, totals, balances, charges, fees, refunds. ISO-4217 currency via `CHAR(3)` column. |
| Utility quantities / 用量·读数 | `DECIMAL(20,6)` | Meter readings (previous/current), consumption, usage_quantity, charge_basis_value. |
| Rates / multipliers / 费率·倍率 | `DECIMAL(20,8)` | unit_price, multiplier, allocation_ratio, escalation_value, allocation_factor, tier thresholds (threshold_from/threshold_to) when used as rate basis. |
| tax_rate / 税率 | `DECIMAL(20,8)` | Treat tax_rate as a rate (fee-rate semantics). |

### Rounding semantics
- Rounding mode: `HALF_UP`.
- Money values carry 2 decimal places (minor unit of the ISO-4217 currency).
- Unit-price / rate intermediate calculations are NOT rounded; only the final money amount is rounded to 2 decimals.
- Aggregation across rows MUST round the final aggregated money, never per-row partials.

### Interval semantics
- All business time intervals are `[start, end)` (half-open): inclusive start, exclusive end.
- Conflict formula: `existing.start < new.end AND existing.end > new.start`.
- Applies to `effective_from/effective_to`, `period_start/period_end`, `start_time/end_time`, `start_date/end_date`, and equivalent schedule ranges.

### Locking & optimistic concurrency
- Optimistic lock column: `version INT NOT NULL DEFAULT 0`. Every mutable business table MUST include it.
- All schedule-changing writes lock `resource_schedule_guard` FIRST in ascending affected resource id order (see `TRANSACTION-LOCK-MATRIX.yaml`).

### Audit fields
- `created_at DATETIME(3) NOT NULL` and `updated_at DATETIME(3) NOT NULL`.
- `created_by BIGINT NULL` and `updated_by BIGINT NULL` (system / bootstrap rows may use NULL).
- Financial fact rows are append-only; corrections use new rows (reversal / red-flush), never UPDATE or DELETE.

### tenant_id discipline
- `tenant_id BIGINT NOT NULL` on every tenant-scoped table.
- Platform-level shared data uses the sentinel `tenant_id = 0` (NEVER NULL). This guarantees unique keys that include `tenant_id` remain effective for shared rows (NULL would silently disable the unique index on MySQL because `NULL != NULL`).
- When a unique key contains `tenant_id`, the sentinel value `0` participates like any other tenant id.
- Pre-verification rows that genuinely cannot carry a verified tenant at insert time (e.g. `payment_callback_log`) MUST use `tenant_id BIGINT NOT NULL DEFAULT 0` plus an `ADD COLUMN verified_tenant_id BIGINT NULL` audit column — see ADR-007 (post-verification backfill) and ADR-020 (shared platform data via sentinel).

## Codegen rule
MyBatis data objects MUST be generated from migration semantics, but domain aggregates MUST NOT be generated as anemic mirrors of the tables.
