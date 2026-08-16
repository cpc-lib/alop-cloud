# PAYMENT STATE MACHINE — V6.3

## 1. PaymentOrder

| Current | Event / Command | Guard | Target |
|---|---|---|---|
| CREATED | CREATE_ATTEMPT | order not expired, no active attempt | PAYING |
| PAYING | PAYMENT_SUCCESS | verified provider transaction | SUCCESS |
| PAYING | CLOSE | provider confirmed unpaid/close success | CLOSED |
| PAYING | ALL_ATTEMPTS_FAILED_AND_EXPIRED | no unknown attempt | CLOSED |
| CLOSED | LATE_PROVIDER_SUCCESS | verified provider success only | SUCCESS |
| SUCCESS | PARTIAL_REFUND_SUCCESS | refunded < paid | PARTIALLY_REFUNDED |
| SUCCESS | FULL_REFUND_SUCCESS | refunded = paid | REFUNDED |
| PARTIALLY_REFUNDED | PARTIAL_REFUND_SUCCESS | refunded < paid | PARTIALLY_REFUNDED |
| PARTIALLY_REFUNDED | FULL_REFUND_SUCCESS | refunded = paid | REFUNDED |

### PaymentOrder Rules
- Order does not expose a generic UNKNOWN state; provider uncertainty lives on `PaymentAttempt` / `RefundOrder`.
- If any active Attempt is UNKNOWN, order remains PAYING and reports `processingState=UNKNOWN` in read model.
- CLOSED -> SUCCESS is only allowed through verified late-provider-success command; never through admin status update.
- SUCCESS/PARTIALLY_REFUNDED/REFUNDED cannot create new payment attempts.

## 2. PaymentAttempt

| Current | Event | Target |
|---|---|---|
| INITIATED | PROVIDER_PREPAY_SUCCESS | PREPAY_CREATED |
| INITIATED | PROVIDER_FAIL | FAILED |
| INITIATED | PROVIDER_UNKNOWN | UNKNOWN |
| PREPAY_CREATED | CLIENT_LAUNCHED | USER_PAYING |
| PREPAY_CREATED | EXPIRE | EXPIRED |
| PREPAY_CREATED | CLOSE | CLOSED |
| USER_PAYING | CALLBACK_SUCCESS | SUCCESS |
| USER_PAYING | QUERY_SUCCESS | SUCCESS |
| USER_PAYING | QUERY_FAIL | FAILED |
| USER_PAYING | QUERY_UNKNOWN | UNKNOWN |
| USER_PAYING | CLOSE | CLOSED |
| UNKNOWN | CALLBACK_SUCCESS | SUCCESS |
| UNKNOWN | QUERY_SUCCESS | SUCCESS |
| UNKNOWN | QUERY_FAIL | FAILED |
| UNKNOWN | QUERY_CLOSED | CLOSED |
| UNKNOWN | QUERY_UNKNOWN | UNKNOWN |

### Active Attempt
`PREPAY_CREATED / USER_PAYING / UNKNOWN` are mutually exclusive as active attempts per PaymentOrder.

## 3. RefundOrder

| Current | Event | Guard | Target |
|---|---|---|---|
| DRAFT | SUBMIT | finance refundable reservation acquired | PENDING_APPROVAL / APPROVED |
| PENDING_APPROVAL | APPROVE | workflow approved + revalidate | APPROVED |
| PENDING_APPROVAL | REJECT | release reservation | CANCELLED |
| APPROVED | SEND_PROVIDER | reservation valid | PROCESSING |
| PROCESSING | PROVIDER_SUCCESS | verified refund fact | SUCCESS |
| PROCESSING | PROVIDER_FAIL | definitive failure | FAILED |
| PROCESSING | PROVIDER_UNKNOWN | result uncertain | UNKNOWN |
| UNKNOWN | CALLBACK_SUCCESS | verified refund fact | SUCCESS |
| UNKNOWN | QUERY_SUCCESS | verified refund fact | SUCCESS |
| UNKNOWN | QUERY_FAIL | definitive failure | FAILED |
| UNKNOWN | QUERY_UNKNOWN | keep reservation | UNKNOWN |
| DRAFT/APPROVED | CANCEL | provider not submitted | CANCELLED |

### Refund Rules
- UNKNOWN never releases Finance refund reservation.
- FAILED/CANCELLED must release reservation idempotently.
- SUCCESS confirms Finance reservation and emits PaymentRefunded exactly once.
- SUCCESS is terminal; corrections are separate financial/accounting operations, never state rollback.

## 4. Callback Decision Matrix

| Local | Provider callback | Action |
|---|---|---|
| PAYING | SUCCESS valid | SUCCESS |
| PAYING | duplicate SUCCESS | idempotent ACK |
| CLOSED | SUCCESS valid | LATE_PROVIDER_SUCCESS + audit |
| SUCCESS | same SUCCESS | idempotent ACK |
| SUCCESS | different tradeNo | P0 duplicate-payment exception |
| any | amount mismatch | reject business mutation, alert |
| any | merchant mismatch | reject business mutation, P0/P1 security alert |
| any | bad signature | reject before deserialization/business mutation |

## 5. Forbidden Transitions
- Admin API -> SUCCESS.
- FAILED/UNKNOWN refund -> SUCCESS without verified callback/query/reconciliation evidence.
- UNKNOWN PaymentAttempt -> new Attempt.
- REFUNDED PaymentOrder -> new payment attempt.
