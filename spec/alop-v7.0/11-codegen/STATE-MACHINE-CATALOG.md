# State Machine Catalog — V7.0

Canonical machine-readable source: `11-codegen/state-machines.yaml`.

## Tenant

Initial: `CREATING`

| Current | Command/Event | Target |
|---|---|---|
| `CREATING` | `activate` | `ACTIVE` |
| `ACTIVE` | `suspend` | `SUSPENDED` |
| `SUSPENDED` | `resume` | `ACTIVE` |
| `ACTIVE` | `expire` | `EXPIRED` |
| `ACTIVE` | `terminateRequest` | `TERMINATING` |
| `SUSPENDED` | `terminateRequest` | `TERMINATING` |
| `EXPIRED` | `terminateRequest` | `TERMINATING` |
| `TERMINATING` | `terminate` | `TERMINATED` |

## Asset

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `PENDING_APPROVAL` | `reject` | `REJECTED` |
| `REJECTED` | `edit` | `DRAFT` |
| `APPROVED` | `startValuation` | `VALUATING` |
| `VALUATING` | `completeValuation` | `VALUATED` |
| `VALUATED` | `activate` | `OPERATING` |
| `OPERATING` | `freeze` | `FROZEN` |
| `FROZEN` | `unfreeze` | `OPERATING` |
| `OPERATING` | `archive` | `ARCHIVED` |

## Reservation

Initial: `PENDING`

| Current | Command/Event | Target |
|---|---|---|
| `PENDING` | `hold` | `HELD` |
| `HELD` | `confirm` | `CONFIRMED` |
| `HELD` | `expire` | `EXPIRED` |
| `HELD` | `cancel` | `CANCELLED` |
| `CONFIRMED` | `convert` | `CONVERTED` |
| `CONFIRMED` | `cancel` | `CANCELLED` |

## Lead

Initial: `NEW`

| Current | Command/Event | Target |
|---|---|---|
| `NEW` | `startContact` | `CONTACTING` |
| `CONTACTING` | `qualify` | `QUALIFIED` |
| `QUALIFIED` | `convert` | `CONVERTED` |
| `NEW` | `invalidate` | `INVALID` |
| `CONTACTING` | `invalidate` | `INVALID` |

## Opportunity

Initial: `NEW`

| Current | Command/Event | Target |
|---|---|---|
| `NEW` | `qualify` | `QUALIFIED` |
| `QUALIFIED` | `startMatching` | `MATCHING` |
| `MATCHING` | `startViewing` | `VIEWING` |
| `VIEWING` | `startQuotation` | `QUOTATION` |
| `QUOTATION` | `negotiate` | `NEGOTIATION` |
| `QUOTATION` | `reserve` | `RESERVATION` |
| `NEGOTIATION` | `reserve` | `RESERVATION` |
| `RESERVATION` | `startContracting` | `CONTRACTING` |
| `CONTRACTING` | `win` | `WON` |

## Viewing

Initial: `PENDING`

| Current | Command/Event | Target |
|---|---|---|
| `PENDING` | `confirm` | `CONFIRMED` |
| `CONFIRMED` | `arrive` | `ARRIVED` |
| `ARRIVED` | `complete` | `COMPLETED` |
| `PENDING` | `cancel` | `CANCELLED` |
| `CONFIRMED` | `cancel` | `CANCELLED` |
| `CONFIRMED` | `noShow` | `NO_SHOW` |

## QuotationVersion

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `PENDING_APPROVAL` | `reject` | `DRAFT` |
| `APPROVED` | `send` | `SENT` |
| `SENT` | `accept` | `ACCEPTED` |
| `SENT` | `rejectByCustomer` | `REJECTED` |
| `SENT` | `expire` | `EXPIRED` |

## Agreement

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `PENDING_APPROVAL` | `reject` | `DRAFT` |
| `APPROVED` | `startSignature` | `WAITING_SIGNATURE` |
| `WAITING_SIGNATURE` | `sign` | `SIGNED` |
| `SIGNED` | `activate` | `EFFECTIVE` |
| `EFFECTIVE` | `markExpiring` | `EXPIRING` |
| `EXPIRING` | `expire` | `EXPIRED` |
| `EFFECTIVE` | `requestTerminate` | `TERMINATING` |
| `EXPIRING` | `requestTerminate` | `TERMINATING` |
| `TERMINATING` | `terminate` | `TERMINATED` |
| `EXPIRED` | `close` | `CLOSED` |
| `TERMINATED` | `close` | `CLOSED` |

## Receivable

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `partialAllocate` | `PARTIALLY_SETTLED` |
| `OPEN` | `settle` | `SETTLED` |
| `PARTIALLY_SETTLED` | `settle` | `SETTLED` |
| `OPEN` | `overdue` | `OVERDUE` |
| `PARTIALLY_SETTLED` | `overdue` | `OVERDUE` |
| `OVERDUE` | `settle` | `SETTLED` |
| `OPEN` | `writeOff` | `WRITTEN_OFF` |
| `OVERDUE` | `writeOff` | `WRITTEN_OFF` |

## PaymentOrder

Initial: `CREATED`

| Current | Command/Event | Target |
|---|---|---|
| `CREATED` | `startPaying` | `PAYING` |
| `PAYING` | `succeed` | `SUCCESS` |
| `PAYING` | `failFinal` | `FAILED` |
| `CREATED` | `close` | `CLOSED` |
| `PAYING` | `closeIfProviderSafe` | `CLOSED` |
| `SUCCESS` | `partialRefund` | `PARTIALLY_REFUNDED` |
| `SUCCESS` | `fullRefund` | `REFUNDED` |
| `PARTIALLY_REFUNDED` | `fullRefund` | `REFUNDED` |

## PaymentAttempt

Initial: `INITIATED`

| Current | Command/Event | Target |
|---|---|---|
| `INITIATED` | `prepayCreated` | `PREPAY_CREATED` |
| `INITIATED` | `providerFail` | `FAILED` |
| `INITIATED` | `uncertain` | `UNKNOWN` |
| `PREPAY_CREATED` | `paid` | `SUCCESS` |
| `PREPAY_CREATED` | `expireOrClose` | `CLOSED` |
| `UNKNOWN` | `querySuccess` | `SUCCESS` |
| `UNKNOWN` | `queryFail` | `FAILED` |
| `UNKNOWN` | `queryClosed` | `CLOSED` |

## InvoiceApplication

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `APPROVED` | `requestProvider` | `WAITING_PROVIDER` |
| `WAITING_PROVIDER` | `submitted` | `SUBMITTED` |
| `SUBMITTED` | `processing` | `PROCESSING` |
| `PROCESSING` | `issue` | `SUCCESS` |
| `PROCESSING` | `fail` | `FAILED` |
| `WAITING_PROVIDER` | `uncertain` | `UNKNOWN` |
| `PROCESSING` | `uncertain` | `UNKNOWN` |
| `UNKNOWN` | `querySuccess` | `SUCCESS` |
| `UNKNOWN` | `queryFail` | `FAILED` |

## SecurityDepositAccount

Initial: `REQUIRED`

| Current | Command/Event | Target |
|---|---|---|
| `REQUIRED` | `partialReceipt` | `PARTIALLY_PAID` |
| `REQUIRED` | `fullReceipt` | `PAID` |
| `PARTIALLY_PAID` | `fullReceipt` | `PAID` |
| `PAID` | `deduct` | `PARTIALLY_DEDUCTED` |
| `PARTIALLY_DEDUCTED` | `refundStart` | `REFUNDING` |
| `PAID` | `refundStart` | `REFUNDING` |
| `REFUNDING` | `refundComplete` | `REFUNDED` |
| `REFUNDED` | `close` | `CLOSED` |
| `PARTIALLY_DEDUCTED` | `closeZeroBalance` | `CLOSED` |

## UnidentifiedCollection

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `proposeMatch` | `MATCH_CANDIDATE` |
| `MATCH_CANDIDATE` | `submitClaim` | `CLAIM_PENDING_APPROVAL` |
| `OPEN` | `submitClaim` | `CLAIM_PENDING_APPROVAL` |
| `CLAIM_PENDING_APPROVAL` | `claim` | `CLAIMED` |
| `OPEN` | `refundRequest` | `REFUND_PENDING` |
| `REFUND_PENDING` | `refund` | `REFUNDED` |
| `CLAIMED` | `close` | `CLOSED` |
| `REFUNDED` | `close` | `CLOSED` |

## ResourceTransfer

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `quote` | `QUOTED` |
| `QUOTED` | `holdTarget` | `TARGET_HELD` |
| `TARGET_HELD` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `APPROVED` | `startSignature` | `WAITING_SIGNATURE` |
| `WAITING_SIGNATURE` | `schedule` | `SCHEDULED` |
| `SCHEDULED` | `execute` | `EXECUTING` |
| `EXECUTING` | `complete` | `COMPLETED` |
| `EXECUTING` | `fail` | `FAILED` |

## TaxRule

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `ACTIVE` |
| `PENDING_APPROVAL` | `reject` | `DRAFT` |
| `ACTIVE` | `expire` | `EXPIRED` |
| `ACTIVE` | `supersede` | `SUPERSEDED` |
| `DRAFT` | `cancel` | `CANCELLED` |

## Payable

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `partialPay` | `PARTIALLY_PAID` |
| `OPEN` | `pay` | `PAID` |
| `PARTIALLY_PAID` | `pay` | `PAID` |
| `OPEN` | `overdue` | `OVERDUE` |
| `PARTIALLY_PAID` | `overdue` | `OVERDUE` |

## OwnerSettlementBatch

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `calculate` | `CALCULATED` |
| `CALCULATED` | `review` | `REVIEWING` |
| `REVIEWING` | `approve` | `APPROVED` |
| `REVIEWING` | `reject` | `REJECTED` |
| `APPROVED` | `createPayable` | `PAYABLE_CREATED` |
| `PAYABLE_CREATED` | `pay` | `PAYING` |
| `PAYING` | `paid` | `PAID` |
| `PAID` | `close` | `CLOSED` |

## BillingPlan

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `activate` | `ACTIVE` |
| `ACTIVE` | `complete` | `COMPLETED` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `ACTIVE` | `cancelFutureOnly` | `CANCELLED` |

## UtilityUsagePeriod

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `calculate` | `CALCULATED` |
| `CALCULATED` | `requireReview` | `REVIEW_REQUIRED` |
| `CALCULATED` | `verify` | `VERIFIED` |
| `REVIEW_REQUIRED` | `verify` | `VERIFIED` |
| `VERIFIED` | `bill` | `BILLED` |
| `BILLED` | `correct` | `CORRECTED` |
| `DRAFT` | `cancel` | `CANCELLED` |

## OperationWorkOrder

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `assign` | `ASSIGNED` |
| `ASSIGNED` | `start` | `IN_PROGRESS` |
| `IN_PROGRESS` | `submitVerification` | `WAITING_VERIFICATION` |
| `WAITING_VERIFICATION` | `verify` | `COMPLETED` |
| `COMPLETED` | `close` | `CLOSED` |
| `OPEN` | `cancel` | `CANCELLED` |
| `ASSIGNED` | `cancel` | `CANCELLED` |

## RenovationOrder

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `PENDING_APPROVAL` | `reject` | `DRAFT` |
| `APPROVED` | `start` | `IN_PROGRESS` |
| `IN_PROGRESS` | `complete` | `COMPLETED` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `APPROVED` | `cancelBeforeStart` | `CANCELLED` |

## IntegrationTask

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `retry` | `RETRYING` |
| `RETRYING` | `resolve` | `RESOLVED` |
| `RETRYING` | `manualRequired` | `WAITING_MANUAL` |
| `WAITING_MANUAL` | `retry` | `RETRYING` |
| `WAITING_MANUAL` | `resolve` | `RESOLVED` |
| `OPEN` | `cancelApproved` | `CANCELLED` |
| `WAITING_MANUAL` | `cancelApproved` | `CANCELLED` |

## TenantMembership

Initial: `ACTIVE`

| Current | Command/Event | Target |
|---|---|---|
| `ACTIVE` | `suspend` | `SUSPENDED` |
| `SUSPENDED` | `resume` | `ACTIVE` |
| `ACTIVE` | `leave` | `LEFT` |
| `SUSPENDED` | `leave` | `LEFT` |

## Customer

Initial: `ACTIVE`

| Current | Command/Event | Target |
|---|---|---|
| `ACTIVE` | `deactivate` | `INACTIVE` |
| `INACTIVE` | `activate` | `ACTIVE` |
| `ACTIVE` | `blacklist` | `BLACKLISTED` |
| `INACTIVE` | `blacklist` | `BLACKLISTED` |
| `BLACKLISTED` | `unblacklist` | `ACTIVE` |
| `ACTIVE` | `mergeInto` | `MERGED` |
| `INACTIVE` | `mergeInto` | `MERGED` |

## HandoverOrder

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `start` | `IN_PROGRESS` |
| `IN_PROGRESS` | `submitConfirm` | `PENDING_CONFIRMATION` |
| `PENDING_CONFIRMATION` | `confirm` | `COMPLETED` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `IN_PROGRESS` | `cancelBeforeFinal` | `CANCELLED` |

