# DDL Contract — V7.0

## Required rules
- Business tables are tenant-scoped unless explicitly platform-global.
- Tenant-scoped unique keys include `tenant_id` unless the business number is intentionally globally unique (e.g. provider-facing payment numbers may additionally have a global unique constraint).
- High-risk IDs and references are BIGINT.
- Money is DECIMAL(18,2); utility quantity/rates use the precision specified in the domain migration.
- Financial fact rows are not physically deleted.
- Every hot tenant query must have a tenant-leading usable index.
- Schedule-changing writes lock `resource_schedule_guard` first in ascending affected resource order.
- Flyway is the only production schema mutation path.
- Zero-downtime change: Expand -> Migrate -> Contract.

## Codegen rule
MyBatis data objects MUST be generated from migration semantics, but domain aggregates MUST NOT be generated as anemic mirrors of the tables.
