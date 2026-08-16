# TASK-029 — Unidentified Collection

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.


## Business Goal
Implement the V6.5 `Unidentified Collection` capability exactly according to the frozen specifications.

## Mandatory Inputs
- `00-master/MASTER-SPEC-V7.0.md`
- corresponding `02-domain/**` SPEC
- related Flyway SQL
- related OpenAPI YAML
- event schemas
- registries
- `08-tests/enterprise-operations-hardening.md`

## Implementation Requirements
1. DDD package tree
2. Aggregate / Entity / ValueObject
3. Commands / Queries / Application handlers
4. Repository ports + MyBatis infrastructure
5. Flyway migration
6. REST/Internal API
7. Outbox/Inbox where defined
8. permissions + tenant isolation
9. audit
10. metrics
11. unit/domain tests
12. integration tests
13. concurrency/failure tests where relevant
14. README
15. SPEC implementation mapping

## Red Lines
- no TODO/pseudocode
- no cross-tenant access
- no destructive financial history edits
- no generic status setter
- no blind retry of UNKNOWN external money movement
- no bypass of ScheduleGuard or Finance local-transaction rules
- no direct SQL repair of historical financial facts

## Definition of Done
All required tests pass, migrations are backward compatible, OpenAPI/Event contracts are consistent, and no unresolved SPEC-GAP remains.
