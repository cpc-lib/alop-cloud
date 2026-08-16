# AGREEMENT DOMAIN SPEC

## 1. Bounded Context / Service
`alop-agreement`

## 2. Aggregate Roots
- `Agreement`
- `AgreementChange`
- `RenewalPriority`
- `HandoverOrder`

## 3. Owned Tables
- `agreement`
- `agreement_item`
- `agreement_snapshot`
- `agreement_change`
- `agreement_sign_process`
- `renewal_priority`
- `handover_order`
- `handover_item`
- `signature_process`

## 4. Commands
- `CreateAgreement`
- `SubmitAgreement`
- `ApproveAgreement`
- `StartSignature`
- `SignAgreement`
- `ActivateAgreement`
- `RequestTermination`
- `CloseAgreement`
- `CreateRenewal`
- `CreateAgreementChange`

## 5. Queries
- `Agreement360`
- `ListExpiringAgreements`

## 6. Produced Events
- `AgreementApproved`
- `AgreementSigned`
- `AgreementEffective`
- `AgreementExpiring`
- `AgreementExpired`
- `AgreementTerminated`
- `AgreementClosed`

## 7. Permissions
- `agreement:create`
- `agreement:approve`
- `agreement:sign`
- `agreement:terminate`

## 8. Invariants
- `signed snapshot immutable`
- `SIGNED != EFFECTIVE`
- `EXPIRED != CLOSED`
- `resource list comes from reservation, not client re-entry`
- `changes create new effective records instead of overwriting history`

## 9. Transaction / Locking
- `agreement FOR UPDATE; sign Saga persistent process`

## 10. Idempotency
- `SignAgreement Idempotency-Key`
- `CommitReservation saga requestId`

## 11. Closure Condition
CLOSED only after handover, occupancy, receivable, deposit, refund, invoice, reconciliation and saga conditions are satisfied.

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


## 16. V6.3 物业费/水电/车位合同要求
- AgreementItem 可同时包含 HOUSE/ROOM/OFFICE 与一个或多个 `PARKING_SPACE`；车位具有独立 unitPrice、period 与 BillingRule。
- 签约快照必须保存：`chargeableAreaSnapshot`、物业管理费费率/周期、UtilityMeter binding identifiers、utility tariff policy identifiers、parking profile snapshot。
- MoveIn/MoveOut Snapshot 必须保存表计读数引用；已签合同后更换车牌通过 `ParkingVehicleBinding` 生效历史处理，不修改 AgreementItem。
- CLOSED 前检查：最终水电结算完成、物业管理费计至终止日、车位 Occupancy/VehicleBinding 已结束。

## V6.4 Reminder Trigger Ownership
Agreement Context owns contract-date reminder triggers; Notification does not scan agreement tables.

`AgreementReminderJob` evaluates tenant reminder policy and emits idempotent triggers such as:
- `agreement.agreement.expiring.v1` with `triggerKey=AGR:{agreementId}:D90`
- D60 / D30 / D15 / D7 / D1
- `agreement.renewal-priority.created.v1`

Reminder generation is idempotent per `tenantId + agreementId + triggerKey`. Notification decides SMS/EMAIL/IN_APP channels and templates.
