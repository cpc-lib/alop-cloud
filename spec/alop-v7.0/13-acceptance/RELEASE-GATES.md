# V7.0 Release Gates

## Contract gates
- every OpenAPI YAML parses;
- every path parameter declared and required;
- operationId unique across all OpenAPI files;
- every Event JSON Schema parses and has eventType/version;
- every TASK references MASTER-SPEC-V7.0;
- every registry YAML parses;
- Flyway table names are unique within a service schema and migration order is deterministic.

## Architecture gates
- ArchUnit: Domain cannot depend on Infrastructure/interfaces.
- Controller cannot depend on Mapper.
- no cross-service DB Mapper/Repository access.
- tenant bypass annotation/package usage restricted to whitelist.

## Critical business gates
- 100 concurrent same-resource reservations -> exactly 1 valid success.
- Redis down -> inventory correctness preserved.
- payment callback x100 -> one PaymentTransaction/Collection effect.
- payment/refund/invoice/AP payout UNKNOWN -> query-before-retry.
- collection/receivable/allocation/ledger concurrency invariant holds.
- invoice quota cannot exceed eligible allocation.
- security deposit cannot refund/deduct above balance.
- 0.01 CNY reconciliation difference -> mismatch.
- whole-unit/room conflict group blocks double inventory.
- Tenant A never reads/writes Tenant B across DB/cache/ES/MQ/file/workflow.

## Quality gates
- core domain coverage >= 90%; application >= 80% for critical modules;
- Testcontainers integration suite green;
- OpenAPI compatibility check green;
- Flyway migration test on empty + previous baseline schema green;
- no unresolved P0/P1 SPEC-GAP.
