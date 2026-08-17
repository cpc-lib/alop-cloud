# OWNER SETTLEMENT DOMAIN SPEC — V7.0

## 1. Bounded Context / Service
`alop-owner-settlement`

For managed-property / agency operating models where the tenant operates external owners' assets and settles eligible revenue to them. OwnerSettlement computes the owner's share of allocated revenue, applies deductions (management commission, property-management / maintenance deductions, taxes / withholding, approved deductions), produces an `OwnerStatement`, and on approval creates an `OwnerPayable` **inside the AP domain** — it never pays the owner directly (ADR-022).

OwnerSettlement **does not** own payout execution or ledger (those are AP / Finance). It owns eligibility, calculation, batching, statement and approval. Every settlement line references a source business record (receivable allocation, bill, expense, tax adjustment).

### Aggregate Roots
- `PropertyOwner` — external owner master.
  Fields: `id`, `tenantId`, `ownerNo`, `ownerType` (`PERSON/COMPANY`), `ownerName`, `taxNo`, `status`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, ownerNo)`.
- `OwnerOperatingAgreement` — binds an owner to an asset for an operating period.
  Fields: `id`, `tenantId`, `ownerId`, `assetId`, `agreementNo`, `effectiveFrom`, `effectiveTo`, `status`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, agreementNo)`; idx `(tenantId, assetId, status)`. Interval `[effectiveFrom, effectiveTo)`.
- `SettlementRule` — versioned calculation rule attached to an operating agreement.
  Fields: `id`, `tenantId`, `ownerOperatingAgreementId`, `ruleType`, `ruleJson` (JSON), `effectiveFrom`, `effectiveTo`, `versionNo`, `status`. UK `(tenantId, ownerOperatingAgreementId, versionNo)`.
  `ruleType`: `FIXED_PERCENTAGE, FIXED_MANAGEMENT_FEE, TIERED_PERCENTAGE, MINIMUM_GUARANTEE, FIXED_OWNER_RENT, REVENUE_SHARE, EXPENSE_PASS_THROUGH`. `ruleJson` carries type-specific params (rates, tiers, fixed fees).
- `OwnerSettlementBatch` — immutable calculation batch for an owner over a period.
  Fields: `id`, `tenantId`, `batchNo`, `ownerId`, `periodStart`, `periodEnd`, `grossEligibleAmount`, `deductionAmount`, `payableAmount`, `currency CHAR(3)`, `status`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, batchNo)`. Period `[periodStart, periodEnd)`. Money `DECIMAL(18,2)`.
- `OwnerSettlementItem` — one calculated line referencing a source business fact.
  Fields: `id`, `tenantId`, `batchId`, `sourceType`, `sourceId`, `grossAmount`, `deductionAmount`, `settlementAmount`, `calculationTraceJson`. UK `(tenantId, batchId, sourceType, sourceId)`.
- `OwnerStatement` — the reviewable statement aggregating one or more batches for approval. Statement approval is the **gate** before `OwnerPayable` creation in AP.

### Calculation Priority Order (positive & negative lines net)
1. **Revenue allocation** — eligible allocated rent/service revenue apportioned to the owner by the active `SettlementRule` at the source fact's business time.
2. **Deduction** — management commission → property-management deduction → maintenance deduction → approved deductions (each may be a positive or negative line).
3. **Tax / withholding adjustment** — tax difference / withholding applied last.
`payableAmount = grossEligibleAmount − deductionAmount`; `deductionAmount` may be negative (owner-favorable) but `payableAmount ≥ 0` is enforced. Every item's `calculationTraceJson` records the rule `versionNo`, rule type, applied rate/amount and the source fact reference, so the line is independently reproducible.

### Worked Example
Owner O1, asset A1, period 2026-07. Eligible allocated rent = 10,000.00; rule = FIXED_PERCENTAGE commission 8%.
- (1) Revenue allocation: owner gross share = 10,000.00.
- (2) Deductions: management commission 800.00 (8%); property-management 200.00; maintenance 50.00 ⇒ deductions = 1,050.00.
- (3) Tax/withholding adjustment: −30.00 (owner-favorable correction).
- `deductionAmount = 1,050.00 − 30.00 = 1,020.00`; `payableAmount = 10,000.00 − 1,020.00 = 8,980.00` (≥ 0).

### Eligibility Rules
A source business fact is eligible for a batch iff:
- it is an allocated/confirmed revenue or approved expense fact (not a draft);
- its business time falls within `[periodStart, periodEnd)` (INV-OS-8) **and** within an ACTIVE `OwnerOperatingAgreement` `[effectiveFrom, effectiveTo)` for the `(owner, asset)` (INV-OS-6);
- it has not already been consumed by any batch for that owner (INV-OS-1);
- a `SettlementRule` version effective at the fact's business time exists for the agreement.
Eligibility is re-evaluated authoritatively at calculate time from Finance/Billing internal APIs; events only trigger refresh.

### Adjustment Batch (post-close correction)
Closed/CALCULATED batches are immutable. Corrections use a new `OwnerSettlementBatch` of adjustment type referencing the original `batchId`, containing reversal items (negating prior lines) plus new corrected items. The adjustment batch goes through its own calculate → statement → approve → AP delta `OwnerPayable` flow. The original batch is never UPDATEd or DELETEd.

## 2. Owned Tables (from flyway V1)
- `property_owner`
- `owner_operating_agreement`
- `owner_settlement_rule`
- `owner_settlement_batch`
- `owner_settlement_item`

## 3. Commands
- `CreatePropertyOwner(ownerType, ownerName, taxNo?)` — `/api/admin/v1/property-owners`. Idempotency: `tenant + Idempotency-Key`; `ownerNo` unique.
- `CreateOperatingAgreement(ownerId, assetId, effectiveFrom, effectiveTo?)`. Pre: owner ACTIVE; no overlapping ACTIVE agreement for same `(ownerId, assetId)` per `[start,end)`. Idempotency: `tenant + Idempotency-Key`.
- `CreateSettlementRule(agreementId, ruleType, ruleJson, effectiveFrom, effectiveTo?)`. Pre: agreement ACTIVE; new `versionNo`; rule params valid for `ruleType`; `[effectiveFrom, effectiveTo)` does not overlap an existing ACTIVE rule for the same agreement. Idempotency: `tenant + agreementId + versionNo`.
- `OwnerSettlementCalculate(ownerId, periodStart, periodEnd, currency)` — `/api/admin/v1/owner-settlements`. Pre: agreement ACTIVE for the period; rules resolved at each source fact's business time (rule version **snapshotted** into `calculationTraceJson`); items computed in priority order; batch `CREATED` → `CALCULATED` (immutable). Idempotency: batch request id `tenant + ownerId + periodStart + periodEnd`.
- `ApproveOwnerStatement(statementId, version, reason)` — `/api/admin/v1/owner-settlements/{id}/approve`. Pre: `status=PENDING_APPROVAL`; `version` matches (optimistic); eligibility finalized. Transition → `APPROVED`. Emits `owner-settlement.statement.approved.v1`. Idempotency: `statementId + version + Idempotency-Key`.
- `CreateAdjustmentBatch(originalBatchId, reason)` — post-close correction: new batch referencing reversal + new items; original batch stays immutable.

## 4. Queries
- `ListPropertyOwners` / `GetOwner`.
- `ListOwnerSettlements(ownerId?, status?)`.
- `GetOwnerSettlementBatch(id)` with items & calculation trace.
- `OwnerSettlement360` (agreement → rules → batches → statement → owner-payable status).
- `EligibilityPreview(ownerId, periodStart, periodEnd)` — dry-run candidate sources without persisting.
- `OwnerEarningsByAsset(assetId, periodStart, periodEnd)`.

## 5. Produced Events
- `owner-settlement.statement.approved.v1` — on `ApproveOwnerStatement`. Payload: `statementId`, `ownerId`, `periodStart`, `periodEnd`, `payableAmount`, `currency`, `batchIds`, rule snapshots. Consumers: `alop-ap` (creates `OwnerPayable` `sourceType=OWNER_SETTLEMENT`, `sourceId=statementId`), `alop-notification`. Delivery at-least-once, Inbox idempotency.
- (Stage event `OwnerSettlementBatchCreated` is emitted via outbox on calculate and referenced by the codegen lock/idempotency matrix; the registry-canonical event is `owner-settlement.statement.approved.v1`.)

## 6. Consumed Events
- `finance.collection.created.v1` / allocation facts — eligible allocated revenue. Events trigger eligibility refresh; the **calculation source of truth is re-fetched authoritatively** from Finance/Billing internal APIs at calculate time (events are not trusted as the calculation input).
- `billing.bill.issued.v1` / `billing.property-fee.billed.v1` / `billing.utility-charge.billed.v1` — revenue source facts.
- `invoice.invoice.issued.v1` / `invoice.invoice.red-flushed.v1` — tax / withholding adjustments.
- Expense / operations facts (maintenance / property-management pass-through deductions).

## 7. Permissions
- `owner:view`
- `owner:manage`
- `owner:settlement:view`
- `owner:settlement:calculate`
- `owner:settlement:approve` (high-risk: gates owner money)

## 8. Invariants (domain-specific, testable)
- INV-OS-1: Same source `(sourceType, sourceId)` cannot be consumed twice across batches for the same owner — enforced by UK `(tenantId, batchId, sourceType, sourceId)` plus a global consumed-source guard per owner.
- INV-OS-2: Rule version is snapshotted into `calculationTraceJson` at calculate time; later rule edits never re-affect a calculated batch.
- INV-OS-3: An approved statement is required before any `OwnerPayable` is created — AP enforces `sourceType=OWNER_SETTLEMENT` + `sourceId=statementId` existence & `APPROVED`.
- INV-OS-4: Closed / `CALCULATED` batches are immutable; corrections use a separate adjustment batch (reversal items + new items).
- INV-OS-5 (priority order): (a) allocate eligible gross revenue by rule, (b) apply deductions in priority (commission → property-management → maintenance → approved), (c) apply tax/withholding adjustment; positive and negative lines net; `payableAmount = grossEligibleAmount − deductionAmount ≥ 0`.
- INV-OS-6: Operating-agreement interval `[effectiveFrom, effectiveTo)`; a source fact's business time must fall within an ACTIVE agreement for the `(owner, asset)` to be eligible. Conflict formula `existing.start < new.end AND existing.end > new.start`.
- INV-OS-7: `OwnerPayable` is generated only via the AP channel; OwnerSettlement never initiates payout directly.
- INV-OS-8 (cross-period): a source fact settles in the period containing the source fact's business time (allocation date), not the statement approval date.
- INV-OS-9: Every settlement item references exactly one source business record (`sourceType, sourceId`); no orphan lines without a source.

## 9. Transaction / Locking
- `OwnerSettlementCalculate`: lock source-eligibility reservation / unique-source guard rows in ascending source-id order; insert immutable items under the same TX; outbox batch-created. Lock `owner_settlement_batch FOR UPDATE` for status transition.
- `ApproveOwnerStatement`: `SELECT ... FROM owner_settlement_batch ... FOR UPDATE` (the batches backing the statement) + statement row `FOR UPDATE`; idempotent transition; outbox `statement.approved`.
- `CreateAdjustmentBatch`: lock original batch `FOR UPDATE` (read-only guard; original immutable) + create new batch/items.
- `CreateOperatingAgreement` / `CreateSettlementRule`: lock existing ACTIVE agreements/rules for `(tenantId, ownerId, assetId)` / `(tenantId, agreementId)` to serialize interval-overlap validation. Optimistic `version`/`versionNo` on approve.

## 10. Idempotency
| Operation | Key |
|---|---|
| CreatePropertyOwner | `tenant + Idempotency-Key` |
| CreateOperatingAgreement | `tenant + Idempotency-Key` |
| CreateSettlementRule | `tenant + agreementId + versionNo` |
| OwnerSettlementCalculate | `tenant + ownerId + periodStart + periodEnd` (batch request id) |
| ApproveOwnerStatement | `statementId + version + Idempotency-Key` |
| CreateAdjustmentBatch | `originalBatchId + correctionRequestId` |

## 11. Closure Condition
A settlement batch is closed when `CALCULATED/APPROVED` and immutable. A statement is closed when `APPROVED` (and the resulting `OwnerPayable` is created in AP) or `REJECTED`. Post-close corrections require a new adjustment batch whose own approval re-enters AP (delta `OwnerPayable`). A period is fully closed when all eligible sources are consumed, the statement is `APPROVED`, the `OwnerPayable` is created, and no pending adjustment remains.

### Reconciliation with AP / Finance
OwnerSettlement does not own the payable ledger. After `owner-settlement.statement.approved.v1`, AP confirms `OwnerPayable` creation back via IntegrationTask; the statement's payable status reflects AP confirmation. Payout, bank reconciliation and ledger entries are owned by AP/Finance. OwnerSettlement only tracks that the owner's share was handed off to AP and is observable via `OwnerSettlement360`.

## 12. Application Pattern
Controller validates DTO and dispatches Command/Query. Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior (eligibility, priority calculation, immutability, approval gate), saves repository and Outbox. Domain holds state transition + invariants; no MyBatis/Redis/RabbitMQ/Flowable dependencies. Eligibility source facts are re-fetched authoritatively from Finance/Billing internal APIs at calculate time (events only trigger refresh). Query side uses read projections under Tenant scope.

## 13. Failure Handling
- Domain conflict → stable business code (`OS_SOURCE_ALREADY_CONSUMED`, `OS_BATCH_IMMUTABLE`, `OS_STATEMENT_NOT_APPROVABLE`, `OS_RULE_INTERVAL_OVERLAP`, `OS_AGREEMENT_NOT_EFFECTIVE`, `OS_PAYABLE_NEGATIVE`).
- Transient DB/external errors retryable only under the idempotency key.
- Cross-context partial success (statement approved → AP `OwnerPayable`) uses persistent Saga/IntegrationTask; AP confirmation feeds back the statement's payable status; no manual SQL repair.
- Post-close corrections use an adjustment batch instead of mutating closed batches.

## 14. Audit & Metrics
- Audit: owner / agreement / rule create, settlement calculate, statement approve / reject, adjustment-batch create, rule-version snapshot. Audit written in the same local TX or reliable Outbox.
- Metrics: `owner_settlement_calculated_total`, `owner_settlement_statement_approved_total{result}`, `owner_settlement_payable_amount_total`, `owner_settlement_adjustment_total`, `owner_settlement_source_conflict_total`, `owner_settlement_latency_seconds`. Avoid `tenantId` as a high-cardinality label.

## 15. Mandatory Tests
1. Eligible revenue allocated by `FIXED_PERCENTAGE` rule; `payable = gross × (1 − commission%)`.
2. `TIERED_PERCENTAGE` / `MINIMUM_GUARANTEE` rule applies the correct tier at the boundary.
3. Calculation priority: revenue allocation → deductions → tax adjustment; `payableAmount = gross − deductions` (net of +/− lines) (INV-OS-5).
4. Same source consumed twice rejected (INV-OS-1).
5. Rule version snapshotted; editing a rule after calculate does not change the batch (INV-OS-2).
6. Closed batch immutable; correction via adjustment batch (INV-OS-4).
7. Statement `APPROVED` → `OwnerPayable` created in AP; no `APPROVED` statement ⇒ no payable (INV-OS-3).
8. `OwnerPayable` paid only via AP payout; OwnerSettlement never triggers direct payout (INV-OS-7).
9. Money balance: `Σ item.settlementAmount − Σ deductions = batch.payableAmount` (`DECIMAL(18,2)`, HALF_UP) (INV-OS-5).
10. Cross-period source settles in the source business-time period, not the approval period (INV-OS-8).
11. Operating-agreement `[start,end)` overlap rejected; adjacent allowed (INV-OS-6).
12. Tenant A cannot see/settle Tenant B owners.
13. `ApproveOwnerStatement` idempotent duplicate → one `OwnerPayable` created in AP.
14. Negative deduction (owner-favorable) nets correctly; `payableAmount ≥ 0` enforced (INV-OS-5/`OS_PAYABLE_NEGATIVE`).
15. Every settlement item references exactly one source record; no orphan lines (INV-OS-9).
16. AP confirms `OwnerPayable` creation back via IntegrationTask; statement payable status reflects AP confirmation; OwnerSettlement never initiates payout directly.
