# TASK-025 Codegen Context Bundle

Title: TASK-025 — Notification Center + Invoice Email Delivery

Dependencies: TASK-001, TASK-002, TASK-003, TASK-016

## Mandatory context files
- `00-master/MASTER-SPEC-V7.0.md`
- `00-master/V7-FREEZE-POLICY.md`
- `01-architecture/architecture-baseline.md`
- `01-architecture/adr/ADR-014-notification-center-invoice-email.md`
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
- `tasks/TASK-025.md`
- `02-domain/notification/DOMAIN-SPEC.md`
- `02-domain/notification/STATE-MACHINE.md`
- `02-domain/notification/PROVIDER-SPEC.md`
- `03-database/flyway/notification/`
- `04-openapi/notification.yaml`
- `02-domain/invoice/DOMAIN-SPEC.md`
- `02-domain/invoice/STATE-MACHINE.md`
- `02-domain/invoice/EMAIL-DELIVERY-SPEC.md`
- `03-database/flyway/invoice/`
- `04-openapi/invoice.yaml`
- `08-tests/notification-invoice-email.md`
- `09-operations/notification-runbook.md`

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
