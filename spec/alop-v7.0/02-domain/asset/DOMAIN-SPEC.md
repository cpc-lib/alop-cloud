# ASSET DOMAIN SPEC

## 1. Bounded Context / Service
`alop-asset`

## 2. Aggregate Roots
- `Asset`
- `ResourceUnit`
- `Listing`
- `RenovationOrder`
- `MaintenanceOrder`

## 3. Owned Tables
- `asset`
- `asset_space`
- `resource_unit`
- `resource_conflict_group`
- `resource_conflict_group_member`
- `valuation`
- `offering`
- `listing`
- `resource_availability`
- `resource_occupancy`
- `resource_schedule_guard`
- `renovation_order`
- `maintenance_order`
- `operation_work_order`
- `parking_space_profile`
- `utility_meter`
- `utility_meter_binding`
- `utility_meter_reading`

## 4. Commands
- `CreateAsset`
- `SubmitAsset`
- `ApproveAsset`
- `CreateResource`
- `CreateOffering`
- `PublishListing`
- `PlanRenovation`
- `CreateMaintenance`
- `CreateParkingSpaceProfile`
- `RegisterUtilityMeter`
- `BindUtilityMeter`
- `SubmitMeterReading`
- `VerifyMeterReading`
- `CorrectMeterReading`

## 5. Queries
- `GetAsset360`
- `SearchResources`
- `GetAvailability`
- `GetParkingAvailability`
- `GetMeterReadings`

## 6. Produced Events
- `AssetApproved`
- `AssetValuated`
- `ListingPublished`
- `ResourceSold`
- `RenovationStarted`
- `MaintenanceStarted`
- `UtilityMeterReadingVerified`
- `UtilityMeterReadingCorrected`
- `ParkingVehicleBound`

## 7. Permissions
- `asset:view`
- `asset:edit`
- `asset:approve`
- `asset:listing:publish`
- `utility:meter:manage`
- `utility:reading:verify`
- `parking:manage`

## 8. Invariants
- `SOLD cannot accept new lease reservation`
- `whole-unit and child-room conflict group must not overlap`
- `offering must be ACTIVE and within validity`
- `MySQL is source of truth; ES is read model`
- `PARKING_SPACE uses the same ScheduleGuard/Reservation/Occupancy rules as rooms/offices`
- `utility readings are immutable after billing; corrections create new versions and adjustment charges`

## 9. Transaction / Locking
- `ScheduleGuard FOR UPDATE for all schedule mutations`

## 10. Idempotency
- `assetCode per tenant`
- `listingNo per tenant`

## 11. Closure Condition
Resource reusable only if not SOLD/FROZEN/RENOVATING/MAINTENANCE and target period has no reservation/occupancy/conflict group conflict.

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
