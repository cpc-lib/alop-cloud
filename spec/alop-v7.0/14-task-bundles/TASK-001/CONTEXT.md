# TASK-001 Codegen Context Bundle

Title: TASK-001 — Platform Foundation

Dependencies: none

## Mandatory context files
- `00-master/MASTER-SPEC-V7.0.md`
- `00-master/V7-FREEZE-POLICY.md`
- `01-architecture/architecture-baseline.md`
- `03-database/DDL-CONTRACT.md`
- `03-database/DATA-DICTIONARY.md`
- `05-events/event-registry.yaml`
- `07-security/tenant-isolation.md`
- `07-security/permissions.md`
- `10-registries/error-codes.yaml`
- `10-registries/permissions.yaml`
- `10-registries/dictionaries.yaml`
- `11-codegen/CODEGEN-CONTRACT.md`
- `11-codegen/TRANSACTION-LOCK-MATRIX.yaml`
- `11-codegen/IDEMPOTENCY-MATRIX.yaml`
- `11-codegen/state-machines.yaml`
- `11-codegen/API-CATALOG.yaml`
- `11-codegen/TRACEABILITY-MATRIX.csv`
- `13-acceptance/RELEASE-GATES.md`
- `13-acceptance/MODULE-DOD-MATRIX.md`
- `tasks/TASK-001.md`
- `02-domain/platform-integration/DOMAIN-SPEC.md`
- `02-domain/platform-integration/STATE-MACHINE.md`
- `03-database/flyway/integration/`

## Agent preflight output
Before generating code, state:
1. service/module to modify;
2. aggregate/facts involved;
3. local transaction boundary and lock order;
4. idempotency guard;
5. APIs/events touched;
6. tenant/permission constraints;
7. unit/integration/concurrency/failure tests;
8. any `SPEC-GAP` found.

## Completion rule
Generated code is not complete until the task DoD and V7 release gates relevant to this module pass.
