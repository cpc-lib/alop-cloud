# TASK-005 — ScheduleGuard + Availability

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.


## Business Goal
ScheduleGuard, Availability, Occupancy conflict checking, conflict-group locking, Redis optional acceleration, concurrency tests.

## Scope / Bounded Context
`alop-asset`

## Preconditions
- MASTER-SPEC-V7.0.md and relevant Domain SPEC have been loaded.
- Error/permission/dictionary registries are available.
- TenantContext is valid; all tenant operations fail closed.

## Required Deliverables
1. Domain model/aggregate/value objects.
2. Application Commands/Queries/Handlers.
3. Infrastructure repository/mapper/adapters.
4. Flyway migration changes.
5. OpenAPI changes.
6. Event JSON Schema changes.
7. Permissions + stable errors.
8. Audit + metrics.
9. Unit/Domain/Integration/Tenant Isolation tests.
10. README + SPEC mapping.

## Transaction / Idempotency
Use the current Domain SPEC. Local state changes are atomic; cross-service side effects use Outbox/Inbox. High-risk write commands require Idempotency-Key or explicit business unique request ID.

## Failure / Compensation
All alternative/failure paths must have stable domain errors. Partial cross-service success requires persisted Saga/IntegrationTask and an idempotent compensation path; no direct production SQL repair.

## Definition of Done
Compile, migrations, tests, OpenAPI/schema validation, tenant isolation, permission/audit/metrics and SPEC mapping all pass; no TODO/placeholder code.
