# TASK-024 — Utilities + Property Management Fee + Parking Leasing

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.


## Business Goal
实现完整水费、电费、物业管理费与车位租赁闭环，禁止仅增加 ChargeType 枚举。

## Scope / Bounded Context
`alop-asset + alop-crm + alop-agreement + alop-billing`，Finance/Payment/Invoice 复用既有闭环。

## Preconditions
- MASTER-SPEC-V7.0.md、ADR-011、`02-domain/utility-property-parking/*` 已加载。
- ResourceScheduleGuard、Agreement、Billing、Finance 基础能力已完成。

## Deliverables
1. UtilityMeter/MeterBinding/MeterReading domain + repositories.
2. ParkingSpaceProfile + CustomerVehicle + ParkingVehicleBinding.
3. UtilityTariffPlan/Tier + PropertyManagementFee BillingRule calculators.
4. Flyway V2 migrations.
5. `utility-parking.yaml` + related APIs.
6. Event JSON Schemas and Outbox/Inbox handlers.
7. Permissions/errors/dictionaries.
8. MoveIn/MoveOut utility settlement integration.
9. Audit/Metrics/Runbook.
10. Unit/Integration/Tenant Isolation/Concurrency/E2E tests.

## Mandatory Invariants
- BILLED meter reading is immutable; correction creates new version and adjustment.
- Only VERIFIED/BILLED valid usage enters billing.
- Shared allocation totals must reconcile to source usage under configured loss policy.
- Property fee uses signed chargeableAreaSnapshot/rate version.
- PARKING_SPACE uses common ScheduleGuard and cannot overlap exclusive occupancy.
- Agreement can contain house/office + parking items in one contract.
- Agreement CLOSED blocked by required final utility settlement or active parking binding.

## Definition of Done
All V6.3 E2E scenarios pass; no TODO/placeholder; YAML/JSON schemas validate; Flyway migrations are additive and tenant-scoped.
