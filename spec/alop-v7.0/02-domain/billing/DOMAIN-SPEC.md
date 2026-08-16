# BILLING DOMAIN SPEC

## 1. Bounded Context / Service
`alop-billing`

## 2. Aggregate Roots
- `BillingPlan`
- `Bill`

## 3. Owned Tables
- `billing_rule`
- `billing_plan`
- `billing_plan_item`
- `bill`
- `bill_item`
- `utility_tariff_plan`
- `utility_tariff_tier`

## 4. Commands
- `CreateBillingRulesFromAgreement`
- `MaterializeBillingPlan`
- `IssueBill`
- `RecalculateFuturePlan`
- `CreateUtilityTariffPlan`
- `CalculateUtilityCharge`
- `CreatePropertyManagementFeeRule`
- `CalculateParkingRent`

## 5. Queries
- `GetBillingPlan`
- `GetBillDetail`
- `PreviewUtilityCharge`
- `PreviewPropertyManagementFee`

## 6. Produced Events
- `BillIssued`
- `BillCancelled`
- `UtilityChargeBilled`
- `PropertyManagementFeeBilled`

## 7. Permissions
- `billing:view`
- `billing:adjust-plan`
- `billing:utility-tariff:manage`
- `billing:property-fee:manage`

## 8. Invariants
- `cycle 1-12 months for long lease`
- `calculation order Base->Proration->Escalation->Discount->Waiver->Tax->Rounding`
- `bill items preserve calculation trace`
- `published historical rules not overwritten`
- `WATER/ELECTRICITY uses verified MeterReading + versioned UtilityTariffPlan`
- `PROPERTY_MANAGEMENT_FEE preserves chargeableAreaSnapshot and rate version`
- `PARKING_RENT can be independent or bundled AgreementItem`

## 9. Transaction / Locking
- `optimistic plan version`

## 10. Idempotency
- `agreementId+planVersion+chargeType+periodStart+periodEnd`

## 11. Closure Condition
Bill is closed when corresponding finance receivable is settled/cancelled/write-off and no pending billing adjustment remains.

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

## V6.4 Bill Notification Trigger
Billing emits `billing.bill.issued.v1`; Notification may send bill-issued EMAIL/IN_APP based on tenant rule. Actual due/overdue reminders should use Finance Receivable triggers so reminder amounts reflect authoritative outstanding balances.
