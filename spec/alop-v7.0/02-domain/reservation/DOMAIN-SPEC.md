# RESERVATION DOMAIN SPEC

## 1. Bounded Context / Service
`alop-reservation`（独立微服务，独立库 `alop_reservation`；与 alop-asset 平级。归属与库拆分依据见 ADR-023）

## 2. Aggregate Roots
- `Reservation`

## 3. Owned Tables
- `reservation`
- `reservation_item`
- `resource_schedule_guard`
- `resource_occupancy`
- `resource_availability`

`resource_conflict_group` / `resource_conflict_group_member` 留在 `alop_asset` 库由 alop-asset 维护，本服务不直接读写，仅通过事件订阅维护本地只读投影（详见 §9）。

## 4. Commands
- `CreateReservation`
- `ConfirmReservation`
- `CancelReservation`
- `ExpireReservation`
- `CommitReservation`
- `ReleaseCommittedReservation`

## 5. Queries
- `GetReservation`
- `CheckResourceAvailability`

## 6. Produced Events
- `ReservationCreated`
- `ReservationConfirmed`
- `ReservationExpired`
- `ReservationConverted`

## 7. Permissions
- `reservation:create`
- `reservation:confirm`
- `reservation:cancel`

## 8. Invariants
- `Customer active and not blacklisted`
- `QuotationVersion accepted and not expired`
- `all items same tenant`
- `all target and conflict-group resources available`
- `multi-resource reservation is all-or-nothing`

## 9. Transaction / Locking
- `Redis optional then ScheduleGuard sorted FOR UPDATE`
- `resource_schedule_guard` 物理表位于本服务库 `alop_reservation`，Reservation/Occupancy/Availability 任何排期变更必须在本服务本地事务内按 `resourceId` 升序 `SELECT ... FOR UPDATE` 锁定目标 `resource_schedule_guard` 行后提交（ADR-002 / ADR-023）。本地事务闭环不依赖跨服务调用 alop-asset。
- `resource_conflict_group` 留在 `alop-asset` 库，本服务不直接读写。本服务订阅 `asset.conflict-group.*` 事件（创建/成员变更/状态切换/解散）在本地维护只读投影（投影表或缓存）用于冲突预检。投影存在最终一致性窗口，因此严格冲突裁决（CommitReservation 时）必须回查 asset 提供的内部 API 或提交 conflict 检查事件，不得只依赖本地投影做权威判断。
- 跨服务的 ConflictGroup 投影同步走 Outbox + Inbox 幂等消费，保证事件至少一次投递且本服务侧不重复应用。

## 10. Idempotency
- `Idempotency-Key`
- `reservationNo`
- `CommitReservation(reservationId,agreementId)`

## 11. Closure Condition
Reservation terminal states EXPIRED/CANCELLED/CONVERTED; late successful deposit after expiry creates ORPHAN_RESERVATION_PAYMENT task, never auto-reactivates.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.

## V6.4 Reservation Reminder Ownership
Reservation Context may emit `asset.reservation.expiring.v1` before `hold_expire_at` (default T-15min, tenant configurable). It must use a stable trigger key and never directly call SMS/email providers. `ReservationExpireJob` remains the authoritative expiry transition job.
