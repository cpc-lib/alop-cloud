# TAX DOMAIN SPEC — V7.0

## 1. Bounded Context / Service
`alop-tax`

Tax domain centralizes tax categories, effective-dated tax rules and the authoritative tax-calculation algorithm consumed by Billing, Invoice, Finance and AP. It is the single source of truth for "which rate/mode applies to a charge category in a jurisdiction at a business time".

Tax domain **does not** compute bill totals, **does not** own bills/receivables, and **does not** persist full bill tax lines — those are immutable snapshots owned by Billing/Invoice. Tax owns the rule registry and the calculation algorithm only. Historical documents are never recalculated (ADR-020).

### Aggregate Roots
- `TaxCategory` — a tenant/platform-scoped charge category code that maps business charges to a tax treatment.
  Fields: `id`, `tenantId` (sentinel `0` = platform-level, per DDL-CONTRACT §tenant_id discipline & ADR-020), `categoryCode`, `categoryName`, `status`, `createdAt`, `updatedAt`. UK `(tenantId, categoryCode)`.
  Invariants: `categoryCode` immutable after creation; `status ∈ {ACTIVE, INACTIVE}`; platform category (`tenantId=0`) is referenceable by all tenants but managed only by platform admin.
- `TaxRule` — an effective-dated, versioned rule for a (scope, jurisdiction, category) tuple.
  Fields: `id`, `tenantId` (`0` = platform), `jurisdictionCode`, `taxCategoryCode`, `taxMode`, `taxRate DECIMAL(20,8)`, `effectiveFrom`, `effectiveUntil`, `versionNo`, `status`, `createdAt`, `updatedAt`. UK `(tenantId, jurisdictionCode, taxCategoryCode, versionNo)`; idx `(tenantId, jurisdictionCode, taxCategoryCode, status, effectiveFrom, effectiveUntil)`.
  Invariants: ACTIVE/PUBLISHED rule immutable (changes create a new `versionNo`); effective interval `[effectiveFrom, effectiveUntil)` half-open; no two ACTIVE rules for the same `(tenantId, jurisdictionCode, taxCategoryCode)` overlap in effective time.

### Tax Modes
- `TAX_EXCLUSIVE` — input amount is net; `tax = net × rate`; `gross = net + tax`.
- `TAX_INCLUSIVE` — input amount is gross; `net = gross / (1 + rate)`; `tax = gross − net`.
- `NON_TAXABLE` (a.k.a. TAX_EXEMPT) — `tax = 0`; `net = gross`; rate snapshot preserved but not applied.

### TAX_INCLUSIVE Split Algorithm & Precision
Per DDL-CONTRACT *Money & Rounding Policy* (HALF_UP, money 2 decimals, unit-price/rate intermediate NOT rounded):
```
net   = gross.divide( BigDecimal.ONE.add(rate), 2, HALF_UP )   // single final rounding to 2dp
tax   = gross.subtract(net)                                      // residue absorbed by tax
gross = net.add(tax)                                             // balances exactly to 2dp
```
- `rate` is `DECIMAL(20,8)`; the division `gross/(1+rate)` is performed at full rate precision and rounded **only once** to 2 decimals (no intermediate per-line rounding).
- Aggregation across rows rounds the **final aggregated** money, never per-row partials.
- For TAX_EXCLUSIVE: `tax = net.multiply(rate)` rounded once to 2dp; `gross = net.add(tax)`.
- The split is deterministic: identical `(gross, rate)` always yields identical `(net, tax)`; residue (if any) is absorbed into `tax` so `net + tax = gross` holds exactly at 2dp.

### Worked Examples (HALF_UP, 2dp)
| mode | input | rate | net | tax | gross |
|---|---|---|---|---|---|
| TAX_EXCLUSIVE | net=100.00 | 0.13 | 100.00 | 13.00 | 113.00 |
| TAX_INCLUSIVE | gross=113.00 | 0.13 | 100.00 | 13.00 | 113.00 |
| TAX_INCLUSIVE | gross=100.00 | 0.033 | 96.81 | 3.19 | 100.00 |
| NON_TAXABLE | amount=100.00 | 0.00 | 100.00 | 0.00 | 100.00 |

### Tax Snapshot Contract (Billing / Invoice)
At bill/invoice issue time the consumer MUST copy, into its own rows, an immutable snapshot of: `taxCategoryCode`, `taxModeSnapshot`, `taxRateSnapshot`, `ruleId`, `ruleVersionNo`, `netAmount`, `taxAmount`, `grossAmount`, `currency`. These snapshot columns are never recomputed from a current rule (INV-TAX-3/INV-TAX-8). A red-flush / reversal produces a new snapshot row with negated amounts, never an in-place mutation.

### Tax Rule Resolution (scope & time)
At `CalculateTax` time, for a given `(tenantId, jurisdictionCode, taxCategoryCode, businessTime)`:
1. Match an ACTIVE rule with `tenantId = <concrete tenant>` whose `[effectiveFrom, effectiveUntil)` contains `businessTime`.
2. If none, fall back to the platform rule (`tenantId = 0`) for the same `(jurisdiction, category)` at `businessTime`.
3. If still none, return `TAX_RULE_NOT_EFFECTIVE` (caller decides NON_TAXABLE fallback vs error).
A tenant rule and a platform rule for the same `(jurisdiction, category)` never "overlap" because they differ in the `tenantId` scope key; the tenant rule simply takes precedence.

## 2. Owned Tables (from flyway V1)
- `tax_category`
- `tax_rule`

## 3. Commands
- `CreateTaxRule(jurisdictionCode, taxCategoryCode, taxMode, taxRate, effectiveFrom, effectiveTo?)`
  Pre: category exists & ACTIVE in scope; no overlapping ACTIVE rule for same `(tenantId, jurisdictionCode, taxCategoryCode)` per `[start,end)`; `taxRate ≥ 0`; mode ∈ `{TAX_EXCLUSIVE, TAX_INCLUSIVE, NON_TAXABLE}`; `effectiveFrom < effectiveTo` (when `effectiveTo` supplied).
  Idempotency: `tenantId + Idempotency-Key`; new `versionNo` assigned under lock.
- `PublishTaxRule(id, version, reason)` — path `/api/admin/v1/tax/rules/{id}/publish`.
  Pre: rule exists, `status=DRAFT`, `version` matches (optimistic); PUBLISH freezes the rule (immutable); `effectiveFrom` not in the past unless platform override.
  Idempotency: `ruleId + versionNo + Idempotency-Key`.
- `CreateTaxCategory(categoryCode, categoryName)` — platform-scoped (`tenantId=0`); platform admin only.
- `CalculateTax(taxCategoryCode, businessTime, amount, currency, amountMode=NET|GROSS)` — internal `/internal/v1/tax/calculate`, pure deterministic query.
  Pre: `amount > 0`; resolves the ACTIVE rule (tenant first, fallback platform `tenantId=0`) at `businessTime`.
  Returns: `taxMode`, `taxRateSnapshot`, `ruleId`, `ruleVersionNo`, `netAmount`, `taxAmount`, `grossAmount` (all `DECIMAL(18,2)`).

## 4. Queries
- `ListTaxRules(category?, status?)` — `/api/admin/v1/tax/rules` GET.
- `GetTaxRule(id)`.
- `ResolveTaxRule(tenantId, jurisdictionCode, taxCategoryCode, businessTime)` — tenant rule first, fallback to platform (`tenantId=0`).
- `ListTaxCategories(scope?)` — platform + tenant categories visible to the caller.

## 5. Produced Events
- `tax.rule.published.v1` — emitted on `PublishTaxRule`. Payload: `ruleId`, `versionNo`, `scope` (platform/tenant), `tenantId`, `jurisdictionCode`, `taxCategoryCode`, `taxMode`, `taxRate`, `effectiveFrom`, `effectiveUntil`. Consumers: `alop-billing`, `alop-invoice` (snapshot the rule for documents issued on/after `effectiveFrom`). Delivery at-least-once, Inbox idempotency. Billing/Invoice snapshot the rule fields into their own rows; they do NOT subscribe to recompute historical documents.

## 6. Consumed Events
None mandatory. Billing/Invoice call the internal `CalculateTax` API at bill-issue time; they do **not** consume tax events to recompute historical documents — historical snapshots are immutable (INV-TAX-3). A new published rule affects only documents issued on/after its `effectiveFrom`.

## 7. Permissions
- `tax:rule:view`
- `tax:rule:manage` (create/draft)
- `tax:rule:publish` (high-risk: freezes a rate that affects future billing)

Platform-level rules (`tenantId=0`) require platform admin; tenant-level rules require tenant finance admin. A tenant user may **view** platform rules but never manage/publish them.

## 8. Invariants (domain-specific, testable)
- INV-TAX-1: For any `(tenantId, jurisdictionCode, taxCategoryCode)`, at most one ACTIVE rule is effective at any `businessTime`. Overlap forbidden via `existing.effectiveFrom < new.effectiveUntil AND existing.effectiveUntil > new.effectiveFrom`.
- INV-TAX-2: A PUBLISHED/ACTIVE rule's `taxMode, taxRate, effectiveFrom, effectiveUntil, jurisdictionCode, taxCategoryCode` are immutable; corrections require a new `versionNo` with a non-overlapping effective interval.
- INV-TAX-3: Historical Bills/Invoices never recalculate using a new rate — they store `tax_category, tax_rate_snapshot, tax_mode_snapshot, net_amount, tax_amount, gross_amount`.
- INV-TAX-4: `net + tax = gross` exactly at 2 decimals (HALF_UP) for every snapshot and calculation result.
- INV-TAX-5: `NON_TAXABLE` yields `tax=0`, `net=gross`.
- INV-TAX-6: A tenant rule with the same `(jurisdiction, category)` at the same `businessTime` takes precedence over the platform rule (`tenantId=0`); they differ in scope key so they do not overlap.
- INV-TAX-7: `taxRate` stored as `DECIMAL(20,8)`; money results `DECIMAL(18,2)`; currency `CHAR(3)` ISO-4217.
- INV-TAX-8: Snapshot freeze — Billing/Invoice copy `ruleId, versionNo, taxRate, taxMode` into their document rows at issue time; subsequent rule publish/versioning never mutates those snapshot columns.
- INV-TAX-9: `currency` is carried on the calculation request/snapshot, never on the rule; a rule is rate/mode only and jurisdiction-scoped. The same rule can serve calculations in any currency (rounding follows the currency minor unit).

## 9. Transaction / Locking
- `CreateTaxRule` / `PublishTaxRule`: `SELECT ... FROM tax_rule WHERE tenant_id=? AND jurisdiction_code=? AND tax_category_code=? AND status='ACTIVE' FOR UPDATE` to serialize overlap validation; assign `versionNo` under the same lock.
- Effective-interval conflict validated with the half-open formula `existing.effective_from < new.effective_until AND existing.effective_until > new.effective_from` (DDL-CONTRACT §Interval semantics).
- `CalculateTax` is read-only snapshot resolution: no write lock, but MUST read a consistent rule version (repeatable-read or `ruleVersionNo` pinned by caller).
- Optimistic concurrency via `version` on publish; mismatch → `TAX_RULE_VERSION_CONFLICT`.

## 10. Idempotency
| Operation | Key |
|---|---|
| CreateTaxRule | `tenantId + Idempotency-Key` |
| PublishTaxRule | `ruleId + versionNo + Idempotency-Key` |
| CreateTaxCategory | `tenantId(=0) + categoryCode` unique |
| CalculateTax | pure function of `(tenantId, jurisdictionCode, taxCategoryCode, businessTime, amount, currency, amountMode)` — deterministic, no side effects |

## 11. Closure Condition
A tax rule is "closed" when `status=INACTIVE` or superseded by a newer ACTIVE version whose `effectiveFrom ≥ this.effectiveUntil`. The tax domain has no per-document closure; bill/invoice tax closure is owned by Billing/Invoice via their immutable snapshots. A category is closed when `status=INACTIVE` and no ACTIVE rule references it.

### Currency & Multi-jurisdiction
`currency` is `CHAR(3)` ISO-4217 carried on the calculation request/snapshot, not on the rule (a rule is rate/mode only and is jurisdiction-scoped, not currency-scoped). When a tenant operates across jurisdictions, the caller supplies `jurisdictionCode` per charge line; `CalculateTax` resolves the rule per `(jurisdiction, category, businessTime)` and returns the money triplet in the requested currency. Rounding always follows the currency's minor unit (2dp for CNY/USD/EUR).

## 12. Application Pattern
Controller validates DTO and dispatches Command/Query. Application loads aggregates, checks tenant/permission (platform vs tenant scope), starts local transaction, invokes Domain behavior (overlap check, version assignment, publish freeze), saves repository and Outbox. Domain holds state transition + invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies. `CalculateTax` is a pure domain service exposed by an internal query endpoint. Query side may use read projections under Tenant scope.

## 13. Failure Handling
- Overlap conflict → stable business code `TAX_RULE_INTERVAL_OVERLAP`.
- Publish version mismatch → `TAX_RULE_VERSION_CONFLICT` (optimistic).
- Unknown category → `TAX_CATEGORY_NOT_FOUND`.
- No effective rule at `businessTime` → `TAX_RULE_NOT_EFFECTIVE` (caller decides NON_TAXABLE fallback vs error).
- Invalid mode/rate → `TAX_RULE_INVALID_PARAMS`.
- Transient DB errors retryable only under the idempotency key.

## 14. Audit & Metrics
- Audit: rule create/publish, category create/status change (platform-scope especially high-risk). Audit written in the same local TX or reliable Outbox.
- Metrics: `tax_rule_published_total{scope}`, `tax_calculate_total{mode}`, `tax_calculate_latency_seconds`, `tax_rule_overlap_conflict_total`, `tax_rule_not_effective_total`. Avoid `tenantId` as a high-cardinality label.

## 15. Mandatory Tests
1. TAX_EXCLUSIVE: `net=100.00 rate=0.13` → `tax=13.00 gross=113.00`.
2. TAX_INCLUSIVE: `gross=113.00 rate=0.13` → `net=100.00 tax=13.00` (`net = gross/(1+rate)`, HALF_UP 2dp).
3. TAX_INCLUSIVE rounding residue: `gross=100.00 rate=0.033` → `net=96.81 tax=3.19`; `net+tax=gross` exactly (residue to tax).
4. NON_TAXABLE: `amount=100.00` → `tax=0 net=100 gross=100`.
5. Overlap rejected: new `[2026-01-01, 2026-04-01)` conflicts with existing `[2026-03-01, 2026-07-01)`.
6. Adjacent non-overlap allowed: `[2026-01-01,2026-04-01)` and `[2026-04-01,2026-07-01)` coexist.
7. Publish freezes rule; subsequent change creates a new `versionNo` (INV-TAX-2).
8. Historical snapshot not recalculated after a new rate publishes (INV-TAX-3/INV-TAX-8).
9. Tenant rule overrides platform rule at the same `businessTime` (INV-TAX-6).
10. `CalculateTax` idempotent & deterministic for identical inputs.
11. Money balance: `net + tax = gross` for all three modes at 2dp HALF_UP (INV-TAX-4).
12. Tenant A cannot view/publish Tenant B rules; platform rule visible to all tenants.
13. Publish with stale `version` → `TAX_RULE_VERSION_CONFLICT` (optimistic).
14. Aggregation rounds final sum only, not per-row partials (DDL-CONTRACT rounding rule).
15. Multi-jurisdiction: the same category in two jurisdictions resolves to independent rules; `currency` is carried on the request/snapshot, not on the rule.
