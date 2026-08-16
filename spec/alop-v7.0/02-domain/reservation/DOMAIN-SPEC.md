# RESERVATION DOMAIN SPEC

## 1. Bounded Context / Service
`alop-asset`

## 2. Aggregate Roots
- `Reservation`

## 3. Owned Tables
- `reservation`
- `reservation_item`
- `resource_schedule_guard`
- `resource_occupancy`
- `resource_availability`

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
