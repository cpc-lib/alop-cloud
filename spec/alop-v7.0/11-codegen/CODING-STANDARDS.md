# Coding Standards

## Java
- Java 21; records are recommended for immutable command/query DTOs and VOs where suitable.
- Enums encode system state; tenant-custom labels stay in business dictionary tables.
- Prefer constructor/factory creation and behavior methods on aggregates.
- `Money` validates currency consistency before arithmetic.
- `TimeRange` enforces `start < end` and `[start,end)` overlap semantics.

## Persistence
- MyBatis-Plus DOs are infrastructure objects, not domain entities.
- Repository ports accept explicit `TenantId` for high-risk business reads/writes.
- Financial tables are append/state-transition only, never soft-delete history.
- Lock ordering is deterministic and specified in transaction-lock-matrix.yaml.

## External integration
- provider requests carry globally unique request numbers;
- log raw payload only where legally/operationally required and always mask secrets/PII;
- uncertain side effect => UNKNOWN + query-before-retry;
- all callbacks preserve body hash/raw evidence according to retention policy.
