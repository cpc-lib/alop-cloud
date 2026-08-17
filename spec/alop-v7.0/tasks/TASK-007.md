# TASK-007 - Reservation Domain V7.0

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.

## ADR-023 Service Boundary (Adjudicated)
- Reservation is an independent service `alop-reservation` with its own database `alop_reservation`.
- It owns exactly five tables: `reservation`, `reservation_item`, `resource_schedule_guard`, `resource_occupancy`, `resource_availability` (currently declared in `03-database/flyway/asset/V1__init.sql`; ownership and migration placement follow ADR-023).
- `resource_conflict_group` / `resource_conflict_group_member` remain in `alop-asset`; `alop-reservation` keeps an event-synced local projection of conflict-group membership to participate in conflict checks. It MUST NOT read or write `alop-asset` tables directly.

## 1. Business Goal
实现生产级多资源预占域：HOLD -> CONFIRM -> CONVERT 生命周期、ResourceScheduleGuard 排他锁下的 [start,end) 冲突检查、整租/分租冲突组互斥、定金到账与过期的竞态裁决、晚到定金 ORPHAN_RESERVATION_PAYMENT、100 线程并发唯一有效预占、多资源全有或全无、租户隔离与可靠事件。

## 2. Bounded Context
`alop-reservation` (module `alop-reservation`, library `alop_reservation`)

## 3. Mandatory Input SPEC
- `00-master/MASTER-SPEC-V7.0.md` (§6.1 房源库存, §11 分布式一致性, §13 技术红线)
- `02-domain/reservation/DOMAIN-SPEC.md`
- `02-domain/reservation/STATE-MACHINE.md`
- `02-domain/asset/DOMAIN-SPEC.md` (conflict group ownership only)
- `01-architecture/adr/ADR-002-resource-schedule-guard.md`
- `01-architecture/adr/ADR-003-resource-conflict-group.md`
- `01-architecture/adr/ADR-023-reservation-service-split.md` (ADR-023)
- `03-database/DDL-CONTRACT.md`
- `03-database/flyway/asset/V1__init.sql` (reservation tables + guard/occupancy/availability)
- `03-database/DATA-DICTIONARY.md`
- `04-openapi/asset.yaml` (reservation endpoints until a dedicated `reservation.yaml` is registered)
- `05-events/registry.md`, `05-events/event-registry.yaml`
- `05-events/schemas/reservation-created-v1.schema.json`, `reservation-expiring-v1.schema.json`, `payment-succeeded-v1.schema.json`, `payment-closed-v1.schema.json`
- `07-security/tenant-isolation.md`, `07-security/permissions.md`
- `08-tests/concurrency.md`, `08-tests/test-plan.md`, `08-tests/chaos.md`
- `12-test-data/reservation-concurrency.json`
- `examples/resource-conflict-sql.md`
- `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `API-CATALOG.yaml`, `JOB-MATRIX.yaml`, `state-machines.yaml`
- `14-task-bundles/TASK-007/CONTEXT.md`

## 4. Aggregate / Entity
### Aggregates
- `Reservation`

### Entities
- `ReservationItem`
- `ResourceScheduleGuard` (per ResourceUnit serialization point)
- `ResourceOccupancy`
- `ResourceAvailability`
- `ResourceConflictGroupProjection` (local projection of alop-asset groups)

### Value Objects
- `ReservationNo`
- `TimeSlot` ([start,end) half-open interval)
- `ResourceUnitRef`
- `HoldPolicy` (hold_expire_at, deposit_required, deposit_amount)
- `ConflictScopeKey` (tenant + resource + group members)

## 5. Commands
Implement at minimum:
- `CreateReservationCommand` (PENDING -> HELD)
- `ConfirmReservationCommand` (HELD -> CONFIRMED, deposit satisfied)
- `CancelReservationCommand` (HELD/CONFIRMED -> CANCELLED)
- `ExpireReservationCommand` (HELD -> EXPIRED, job only)
- `CommitReservationCommand` (CONFIRMED -> CONVERTED, internal saga step)
- `ReleaseCommittedReservationCommand` (idempotent saga compensation)
- `RecordDepositPaidCommand` (consumes verified payment fact; arbitrates EXPIRED vs CONFIRMED)

## 6. Queries
- `GetReservation` (detail with items/deposit summary)
- `CheckResourceAvailability` (GET /api/admin/v1/resources/{resourceId}/availability)
- reservation list by customer/opportunity (tenant scoped)

## 7. Application Flow - Create Reservation
1. Validate TenantContext; fail closed without tenant.
2. Validate customer belongs to tenant, is ACTIVE and not BLACKLISTED.
3. Validate quotationVersionId ACCEPTED and not expired; opportunity belongs to tenant.
4. Collect target resource ids from items; expand each id with its conflict-group projection members (MUTUAL_EXCLUSIVE / PARENT_CHILD_EXCLUSIVE).
5. Optional Redis fast-fail on obvious conflicts; Redis is acceleration only, never the final decision.
6. Local transaction BEGIN:
   - Lock `resource_schedule_guard` rows for ALL affected resources (targets + conflict-group members) ordered by resourceUnitId ASC `SELECT ... FOR UPDATE` (ADR-002).
   - Re-check resource statuses: SOLD/FROZEN/RENOVATING/MAINTENANCE/ARCHIVED block new reservation in overlapping window (MASTER-SPEC §6.1).
   - Conflict check with half-open predicate `existing.start < new.end AND existing.end > new.start` against `resource_occupancy` (PLANNED/ACTIVE) and HELD/CONFIRMED reservations (see `examples/resource-conflict-sql.md`).
   - Multi-resource reservation is all-or-nothing: any item conflict rolls back everything.
   - Insert `reservation` (HELD, hold_expire_at, deposit policy) + `reservation_item` rows.
   - Update guard `schedule_version`.
   - Audit + Outbox `asset.reservation.created.v1`.
7. COMMIT; return reservation detail.
8. Duplicate Idempotency-Key returns the original reservation (no double hold).

## 8. Application Flow - Confirm / Deposit Race
1. Deposit-required reservation stays HELD until verified payment fact arrives.
2. `RecordDepositPaidCommand` and `ExpireReservationCommand` both take `reservation ... FOR UPDATE`.
3. If still HELD: accumulate `deposit_paid_amount`; once `deposit_paid_amount >= deposit_amount` -> CONFIRMED.
4. If already EXPIRED when the deposit fact arrives: DO NOT reactivate; route to ORPHAN_RESERVATION_PAYMENT (see §9).
5. Exactly one of EXPIRED / CONFIRMED wins the race (08-tests/concurrency.md case 5).

## 9. Expiry & Late Deposit (ORPHAN_RESERVATION_PAYMENT)
1. `ReservationExpireJob` scans HELD with `hold_expire_at < now`; CAS HELD -> EXPIRED under row lock; releases nothing else (guards are advisory serialization points only).
2. Expiring reminder: emit `asset.reservation.expiring.v1` before `hold_expire_at` (default T-15min, tenant configurable) with a stable trigger key; never call SMS/email providers directly.
3. Consume `payment.payment.succeeded.v1` via Inbox when business relation targets a reservation deposit:
   - reservation HELD -> §8 confirm path;
   - reservation EXPIRED/CANCELLED/CONVERTED -> create `IntegrationTask` `ORPHAN_RESERVATION_PAYMENT` referencing paymentNo; the money stays with finance (unallocated path), never auto-refund from reservation service.
4. Consume `payment.payment.closed.v1` to stop waiting for an unpaid deposit order.
5. Duplicate event deliveries x100 must produce exactly one effect.

## 10. Commit / Release for Sign Saga (internal)
- Internal endpoint `POST /internal/v1/reservations/{reservationId}/commit` (operationId `postReservationsReservationIdCommit`) invoked by alop-agreement Sign Saga (ADR-006, TASK-012).
- Commit: guard lock ASC -> reservation FOR UPDATE -> verify CONFIRMED -> transition CONVERTED + write `resource_occupancy` (source_type=AGREEMENT) in the same local transaction; idempotent on (reservationId, agreementId, requestId).
- ReleaseCommittedReservation: idempotent compensation that ends occupancy and rolls reservation back to CONFIRMED (no physical delete; occupancy status -> RELEASED).
- Never expose commit/release on admin/app surfaces.

## 11. Conflict Group Projection
- Subscribe to alop-asset conflict-group membership events (Inbox, at-least-once).
- Materialize membership into a local projection table inside `alop_reservation`; projection lag must degrade to fail-closed (re-check via internal Asset API before allowing a hold when the projection is stale).
- alop-asset remains the owner of group definitions; this service never writes group tables.

## 12. Database Deliverables
Flyway migrations for library `alop_reservation` (see ADR-023 note in header). Generate MyBatis DO/Mapper/Repository for:
- `reservation`
- `reservation_item`
- `resource_schedule_guard`
- `resource_occupancy`
- `resource_availability`
- conflict-group projection table (new, tenant-scoped, event-synced)

All tenant-scoped unique keys include `tenant_id` (DDL-CONTRACT).

## 13. Idempotency
- Create Reservation: Idempotency-Key (24h) + `reservationNo` unique.
- Confirm/Cancel: Idempotency-Key + state-machine guard.
- Commit: (reservationId, agreementId, saga requestId) unique.
- Release: saga compensation id unique.
- Deposit fact: Inbox (consumerGroup + eventId) + paymentNo.
- Expire job: CAS HELD -> EXPIRED.

## 14. Events (Outbox)
Produce exactly (schema files under `05-events/schemas/`):
- `asset.reservation.created.v1`
- `asset.reservation.expiring.v1`
- `asset.reservation.expired.v1`
Internal transitions without registry entries (confirm/convert/release) stay `internal-only` until registered.

## 15. Permissions
- `reservation:create`
- `reservation:confirm`
- `reservation:cancel`
- `reservation:view`
Internal commit/release uses trusted internal authentication, not user permissions.

## 16. Required Metrics
- create success/failure by domain error code, latency
- schedule-guard lock wait time and deadlocks
- conflict rejections by reason (occupied / conflict-group / status-blocked)
- expired holds, orphan deposit tasks, projection lag

## 17. Tests - Must Pass
### Domain (no Spring)
- Reservation state matrix (PENDING/HELD/CONFIRMED/EXPIRED/CANCELLED/CONVERTED) and invalid transitions.
- Overlap predicate boundary tests: touching intervals [1,2) vs [2,3) do NOT conflict.

### Concurrency (load `12-test-data/reservation-concurrency.json`)
- 100 threads, tenant 1001, resource 801, 2027-01-01..2028-01-01: exactly 1 success, 99 stable conflict errors, `expectedValidReservations=1`.
- Redis unavailable -> identical correctness via MySQL ScheduleGuard.
- Multi-resource A+B+C where B conflicts -> zero items committed.
- Whole-unit vs room conflict-group mutual exclusion, both directions.
- Expiry vs deposit confirmation race -> single winner.

### Idempotency
- Duplicate create with same Idempotency-Key x10 -> one reservation.
- Duplicate `payment.payment.succeeded.v1` x100 -> one deposit effect.
- Commit called twice with same saga requestId -> one CONVERTED.

### Tenant Isolation
- Tenant A cannot read/confirm/commit Tenant B reservations; forged tenant header rejected.
- Cross-tenant resource ids in items rejected even when the resource exists.

### Integration (Testcontainers MySQL/RabbitMQ)
- Persistence round-trip for all tables; Outbox publishes; Inbox dedupes.
- Orphan deposit path creates IntegrationTask `ORPHAN_RESERVATION_PAYMENT` and does not touch finance tables.

### Coverage Targets
- Domain >= 90% line/branch on invariants and state transitions.
- Application/integration >= 80% targeted orchestration and error paths.

## 18. Forbidden Implementation
- Redis lock as the only serialization point.
- Locking guards in non-deterministic order (deadlock).
- Direct read/write of `alop-asset` tables (including `resource_conflict_group`).
- Generic UPDATE API that sets reservation status.
- Reopening terminal states; auto-reactivating expired reservations on late money.
- Calling SMS/email providers from reservation service.
- Deleting occupancy history rows physically.

## 19. Definition of Done
- Compile; Flyway applies cleanly in `alop_reservation`.
- OpenAPI contract tests pass for reservation endpoints.
- Event schemas validate.
- Domain >= 90%, application/integration >= 80% coverage.
- 100-thread reservation test passes with exactly 1 winner.
- Deposit race, orphan deposit, commit/release saga idempotency pass.
- Tenant isolation pass; permission/audit/metrics wired.
- No TODO/placeholder code.

## 20. SPEC Mapping
| Requirement | SPEC Source |
|---|---|
| Guard lock order + FOR UPDATE | ADR-002; DDL-CONTRACT.md |
| Conflict group projection | ADR-003; ADR-023 |
| [start,end) overlap formula | MASTER-SPEC-V7.0 §6.1; examples/resource-conflict-sql.md |
| Reservation state machine | 02-domain/reservation/STATE-MACHINE.md; 11-codegen/state-machines.yaml |
| All-or-nothing multi-resource | 02-domain/reservation/DOMAIN-SPEC.md §8 |
| ORPHAN_RESERVATION_PAYMENT | 02-domain/reservation/DOMAIN-SPEC.md §11 |
| Expiry job | 11-codegen/JOB-MATRIX.yaml (ReservationExpireJob) |
| Expiring reminder T-15min | 02-domain/reservation/DOMAIN-SPEC.md (V6.4 ownership) |
| Saga commit/compensation | ADR-006; MASTER-SPEC §11 |
| 100-thread concurrency | 08-tests/concurrency.md #1-#5; 12-test-data/reservation-concurrency.json |
