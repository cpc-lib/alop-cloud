# ACCOUNTS PAYABLE (AP) DOMAIN SPEC — V7.0

## 1. Bounded Context / Service
`alop-ap`

AP manages money the tenant owes to suppliers, brokers, utility/property vendors, owners (via OwnerSettlement) and other counterparties. AP is intentionally separate from customer AR (Finance Receivable): a customer Receivable is **never** reused as a supplier Payable (ADR-021).

AP **does not** own bank/channel reconciliation of customer payments (that is Finance) and **does not** issue invoices to customers. It owns the supplier master, payable lifecycle, payment-request approval and payout execution.

### Aggregate Roots
- `Supplier` — master counterparty record.
  Fields (from DDL): `id`, `tenantId`, `supplierNo`, `supplierName`, `supplierType`, `taxNo`, `bankName`, `bankAccountCiphertext`, `bankAccountHash`, `status`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, supplierNo)`.
  Invariants: bank account stored as ciphertext + hash (never plaintext); `status ∈ {ACTIVE, INACTIVE}`; `supplierNo` immutable; `supplierType` distinguishes (`VENDOR, BROKER, UTILITY, OWNER, OTHER`).
- `Payable` — a verified obligation the tenant owes a supplier.
  Fields: `id`, `tenantId`, `payableNo`, `supplierId`, `sourceType`, `sourceId`, `currency CHAR(3)`, `originalAmount`, `adjustmentAmount`, `payableAmount`, `paidAmount`, `outstandingAmount`, `dueDate`, `status`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, payableNo)`. All money `DECIMAL(18,2)`.
  Invariants: `payableAmount = originalAmount + adjustmentAmount`; `outstandingAmount = payableAmount − paidAmount ≥ 0`; `(sourceType, sourceId)` consumed at most once per tenant.
- `PaymentRequest` — a request to pay one or more payables, subject to approval.
  Fields: `id`, `tenantId`, `requestNo`, `supplierId`, `currency`, `requestedAmount`, `status`, `workflowInstanceId`, `version`, `createdAt`, `updatedAt`. UK `(tenantId, requestNo)`. Items in `payment_request_item` (`paymentRequestId, payableId, amount`), UK `(tenantId, paymentRequestId, payableId)`.
  Invariants: `Σ item.amount = requestedAmount`; each `item.amount ≤ corresponding payable.outstandingAmount` at submit **and** at execute; at most one ACTIVE/PENDING_APPROVAL request per payable.
- `PayoutOrder` — the external money-movement execution to a bank/provider channel. Execution-stage aggregate; its create/query/close requests and confirmed facts are audited using the **same discipline as the payment domain** (`PaymentChannelRequest`-style request audit + payout transaction by provider unique key `(channel, providerMerchantId, channelTradeNo)`). Payout fact tables are owned by AP. Fields: `payoutNo`, `requestId`, `channel`, `providerRequestNo`, `providerStatus`, `amount`, `currency`, `status`, `unknownSince`, `lastQueryAt`, `queryCount`, `version`.

### Payable Lifecycle
```
OPEN -> PARTIALLY_PAID -> PAID
OPEN/PARTIALLY_PAID -> OVERDUE   (past dueDate, monthly closing / 账期)
OVERDUE -> PAID                   (late payment allowed)
OPEN/PARTIALLY_PAID/OVERDUE -> CANCELLED
OPEN/PARTIALLY_PAID/OVERDUE -> WRITTEN_OFF   (approved)
```
`PAID / CANCELLED / WRITTEN_OFF` are terminal. No `setStatus()`; transitions are behavior methods.

### Accounting
Recognition: `Dr expense/asset`, `Cr ACCOUNTS_PAYABLE`.
Payout success: `Dr ACCOUNTS_PAYABLE`, `Cr BANK`.
Write-off: `Dr bad-debt expense`, `Cr ACCOUNTS_PAYABLE` (via new reversal rows, never UPDATE/DELETE of facts).

## 2. Owned Tables (from flyway V1)
- `supplier`
- `payable`
- `payment_request`
- `payment_request_item`

## 3. Commands
- `CreateSupplier(supplierName, supplierType, taxNo?, bankName?, bankAccountToken?)` — `/api/admin/v1/suppliers`.
  Pre: `supplierNo` unique in tenant; bank account tokenized (ciphertext+hash, never plaintext). Idempotency: `tenantId + Idempotency-Key`.
- `CreatePayable(supplierId, sourceType, sourceId, currency, originalAmount, dueDate)`.
  Pre: supplier ACTIVE; `(sourceType, sourceId)` not already consumed in tenant; `originalAmount > 0`. Idempotency: `tenantId + sourceType + sourceId` (source-keyed) or `Idempotency-Key`. Emits `ap.payable.created.v1`.
- `AdjustPayable(payableId, adjustmentAmount, reason)`.
  Pre: payable not terminal; `adjustmentAmount` may be +/−; new `payableAmount` recalculated; `outstandingAmount ≥ 0`. Idempotency: `payableId + adjustmentRequestId`.
- `CreateAPPaymentRequest(supplierId, currency, items[], reason?)` — `/api/admin/v1/payment-requests`.
  Pre: each `item.payableId` belongs to the supplier & not terminal; `item.amount ≤ payable.outstandingAmount`; `Σ items = requestedAmount`; no other ACTIVE/PENDING_APPROVAL request covers the same payable. Lock: payable ids ASC `FOR UPDATE`. Idempotency: `tenantId + Idempotency-Key`.
- `ApprovePaymentRequest(requestId, workflowInstanceId)` — consumed from Flowable workflow callback.
  Pre: `status=PENDING_APPROVAL`; re-verify outstanding amounts under lock; transition to `APPROVED`. AP re-validates on APPROVED (workflow only drives approval, never trusts workflow as the final guard).
- `ExecutePayout(requestId)`.
  Pre: request `APPROVED`; `ReservePayoutAmount` succeeds (pre-occupy payable outstanding — mirrors Finance refund reservation); submit to bank/provider channel.
  UNKNOWN safety (mirror payment domain): channel UNKNOWN → **keep reservation, query provider, no blind retry, no channel switch** until resolved. Idempotency: `providerRequestNo`.
- `CancelPayable(payableId, reason)` / `WriteOffPayable(payableId, reason, approval?)` — terminal transitions.

### Payout UNKNOWN Closed Loop (mirrors payment domain §15)
```
ExecutePayout -> Provider.create
  SUCCESS -> PayoutTransaction + PayoutOrder SUCCESS + Payable paid + Outbox PayoutSucceeded
  FAILED  -> PayoutOrder FAILED + release payout reservation
  UNKNOWN -> PayoutOrder UNKNOWN + keep reservation
            -> PayoutUnknownQueryJob -> Provider.query
               SUCCESS -> normal success fact
               FAILED/CLOSED -> fail/close payout, release reservation
               still unknown -> exponential query schedule
               threshold exceeded -> IntegrationTask WAITING_MANUAL
```
UNKNOWN forbids: creating a new channel attempt; manual mark-SUCCESS; auto-retry of the same money movement; treating the request as FAILED. Late provider success after local fail uses a dedicated `recordLateSuccess()` path (no public mark-SUCCESS API).

## 4. Queries
- `ListSuppliers` / `GetSupplier`.
- `ListPayables(supplierId?, status?)` — `/api/admin/v1/payables`.
- `GetPayable` with paid/outstanding summary.
- `ListPaymentRequests(status?)`.
- `PayableAging` (by `dueDate` buckets, monthly closing / 账期).
- `AP360` (supplier → payables → requests → payouts).
- `ListPayoutExceptions` (UNKNOWN / late-success / amount-mismatch).

## 5. Produced Events
- `ap.payable.created.v1` — on `CreatePayable`; consumer `alop-notification` (notify supplier/owner). Delivery at-least-once, Inbox idempotency.
- (Stage events `PaymentRequestCreated` / `PayoutSucceeded` are emitted via outbox at the corresponding stage and referenced by the codegen lock/idempotency matrix; the registry-canonical AP event is `ap.payable.created.v1`.)

## 6. Consumed Events
- `owner-settlement.statement.approved.v1` — AP creates an `OwnerPayable` (`sourceType=OWNER_SETTLEMENT`, `sourceId=statementId`) for the approved statement amount; the owner is **never** paid directly outside AP.
- `billing.utility-charge.billed.v1` / supplier-invoice sources — AP may create a Payable from verified supplier invoices (`sourceType=SUPPLIER_INVOICE`).
- Flowable workflow approval callbacks — drive `PaymentRequest` `PENDING_APPROVAL → APPROVED`.

## 7. Permissions
- `ap:supplier:view`
- `ap:supplier:manage`
- `ap:payable:view`
- `ap:payment-request:create`
- `ap:payment-request:approve` (high-risk)
- `ap:payout:execute` (high-risk)

## 8. Invariants (domain-specific, testable)
- INV-AP-1 (AR/AP separation): a customer Receivable is never reused as a supplier Payable; `sourceType` distinguishes (`SUPPLIER_INVOICE, OWNER_SETTLEMENT, UTILITY, PROPERTY_FEE, COMMISSION, …`).
- INV-AP-2: `payableAmount = originalAmount + adjustmentAmount`; `outstandingAmount = payableAmount − paidAmount ≥ 0`.
- INV-AP-3: `Σ confirmed payouts ≤ approved outstanding payable` (per payable and per request).
- INV-AP-4: Same `(tenantId, sourceType, sourceId)` consumed at most once — duplicate supplier invoices detected & rejected.
- INV-AP-5 (UNKNOWN safety): a payout UNKNOWN is queried, not blindly retried; channel switching is forbidden while UNKNOWN.
- INV-AP-6 (Payout reservation): payout requires `ReservePayoutAmount` pre-occupy; reservation **kept on UNKNOWN**, **released on definitive FAILED/CANCELLED**, **confirmed on SUCCESS** (mirrors Finance refund reservation).
- INV-AP-7: history uses reversal/adjustment rows; financial fact rows are never physically deleted.
- INV-AP-8: strict tenant isolation (`tenant_id` leading every unique key & index).
- INV-AP-9 (monthly closing / 账期): `OVERDUE` transition driven by `dueDate`; `OVERDUE → PAID` allowed (late payment).

## 9. Transaction / Locking
- `CreateAPPaymentRequest`: lock payables by id ASC `FOR UPDATE`; validate outstanding under lock; insert request + items.
- `ExecutePayout`: lock `payment_request FOR UPDATE`; re-check outstanding; reserve payout amount; outbox payout intent + channel request. When multiple payables settle in one request, apply in `payableId ASC`.
- Payout callback/query confirmation: lock `payment_request FOR UPDATE`; idempotent state transition; upsert payout fact by provider unique key `(channel, providerMerchantId, channelTradeNo)` (mirror payment domain).
- Reservation lifecycle (`ReservePayoutAmount` / confirm / release) is itself idempotent and row-locked on the reservation id.

## 10. Idempotency
| Operation | Primary Key | Secondary Guard |
|---|---|---|
| CreateSupplier | `tenant + Idempotency-Key` | `supplierNo` unique |
| CreatePayable | `tenant + sourceType + sourceId` | `Idempotency-Key` |
| CreateAPPaymentRequest | `tenant + Idempotency-Key` | one active request per payable |
| ExecutePayout | `providerRequestNo` | provider payout tradeNo unique |
| Payout callback | provider callback/body hash | `providerRefNo` unique |
| ApprovePaymentRequest | `requestId + workflowDecisionId` | state guard |

## 11. Closure Condition
A Payable is closed when `status ∈ {PAID, CANCELLED, WRITTEN_OFF}` AND (`outstandingAmount = 0` for PAID, or approved write-off/cancel) AND no in-flight UNKNOWN payout AND no active payout reservation. A PaymentRequest is closed when terminal (`PAID/FAILED/CANCELLED`) with all payout facts resolved (UNKNOWN resolved to SUCCESS/FAILED). **UNKNOWN never counts as closed.** Monthly closing (月结) requires every non-terminal Payable past `dueDate` to be either paid, written off, or explicitly carried forward.

## 12. Application Pattern
Controller validates DTO and dispatches Command/Query. Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox. Domain holds state transition + invariants; no MyBatis/Redis/RabbitMQ/Flowable dependencies. Flowable only drives approval; AP re-validates on APPROVED. Query side uses read projections under Tenant scope.

## 13. Failure Handling
- Domain conflict → stable business code (`AP_PAYABLE_ALREADY_CONSUMED`, `AP_PAYOUT_EXCEEDS_OUTSTANDING`, `AP_REQUEST_OVERLAPS_ACTIVE`, `AP_BANK_ACCOUNT_FORBIDDEN`, `AP_PAYABLE_TERMINAL`).
- Channel UNKNOWN → keep reservation, schedule `PayoutUnknownQueryJob`, no blind retry, no channel switch (mirror payment domain §15).
- Late provider success after local fail/close → `recordLateSuccess()` path, create/confirm payout fact, emit `PayoutSucceeded`, no manual mark-SUCCESS API.
- Transient DB/external errors retryable only when idempotent.
- Cross-context partial success (owner-settlement → AP payable) uses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- Audit: supplier create / bank-account change, payable create/adjust/write-off, payment-request create/approve, payout execute / UNKNOWN / late-success, manual repair. Audit written in the same local TX or reliable Outbox.
- Metrics: `ap_payable_created_total{sourceType}`, `ap_payout_total{channel,result}`, `ap_payout_unknown_total`, `ap_payout_unknown_age_seconds`, `ap_payable_overdue_total`, `ap_payment_request_approved_total`, `ap_amount_mismatch_total`, `ap_payout_late_success_total`. Avoid `tenantId` as a high-cardinality label.
- Bank account number / tax secret must never appear in logs or audit payload (ciphertext + hash only).

## 15. Mandatory Tests
1. `CreatePayable` → `outstanding = original`; a customer AR record cannot be loaded as a Payable (AR/AP separation).
2. Duplicate supplier invoice (same `sourceType+sourceId`) rejected (INV-AP-4).
3. `PaymentRequest` items `Σ = requestedAmount`; `item > outstanding` rejected.
4. Overlapping active request on the same payable rejected.
5. Payout SUCCESS → Payable `PAID` (or `PARTIALLY_PAID`), `outstanding` decreases, ledger `Dr AP / Cr Bank`.
6. Payout UNKNOWN → reservation kept, no blind retry, no channel switch; query resolves to SUCCESS then PAID (INV-AP-5/6).
7. Payout FAILED → reservation released, Payable stays OPEN.
8. Concurrent payouts on the same payable cannot exceed `outstanding` (balance test, INV-AP-3).
9. `OVERDUE` transition on `dueDate`; `OVERDUE → PAID` allowed (INV-AP-9).
10. Write-off / Cancel terminal + immutable history (reversal via new row, INV-AP-7).
11. `owner-settlement.statement.approved.v1` → `OwnerPayable` created in AP; owner paid only via AP payout (no bypass, INV-AP-1).
12. Tenant A cannot see/pay Tenant B payables (INV-AP-8).
13. Bank account never stored/logged as plaintext.
14. Money fields `DECIMAL(18,2)`; payout amount exact-match (no 0.01 tolerance).
15. Late provider success after local FAILED → `recordLateSuccess()` creates payout fact + `PayoutSucceeded`; no public mark-SUCCESS API.
