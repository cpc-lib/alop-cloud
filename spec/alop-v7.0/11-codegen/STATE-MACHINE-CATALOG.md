# State Machine Catalog - V7.0

Canonical machine-readable source: `11-codegen/state-machines.yaml` (frozen codegen contract). This catalog is its human-readable mirror; on conflict the yaml wins. Any machine change requires an ADR and must update the yaml, this catalog, and the owning domain's STATE-MACHINE.md together.

Total machines: **34**

| # | Machine | Domain | Canonical source |
|---|---|---|---|
| 1 | Tenant | tenant | `11-codegen/state-machines.yaml` |
| 2 | Asset | asset | `11-codegen/state-machines.yaml` |
| 3 | Reservation | asset | `11-codegen/state-machines.yaml` |
| 4 | Lead | crm | `11-codegen/state-machines.yaml` |
| 5 | Opportunity | crm | `11-codegen/state-machines.yaml` |
| 6 | Viewing | crm | `11-codegen/state-machines.yaml` |
| 7 | QuotationVersion | crm | `11-codegen/state-machines.yaml` |
| 8 | Agreement | agreement | `11-codegen/state-machines.yaml` |
| 9 | Receivable | finance | `11-codegen/state-machines.yaml` |
| 10 | PaymentOrder | payment | `11-codegen/state-machines.yaml` |
| 11 | PaymentAttempt | payment | `11-codegen/state-machines.yaml` |
| 12 | RefundOrder | payment | `11-codegen/state-machines.yaml` |
| 13 | InvoiceApplication | invoice | `11-codegen/state-machines.yaml` |
| 14 | RedFlushApplication | invoice | `11-codegen/state-machines.yaml` |
| 15 | InvoiceDeliveryInstruction | invoice | `11-codegen/state-machines.yaml` |
| 16 | SecurityDepositAccount | finance | `11-codegen/state-machines.yaml` |
| 17 | UnidentifiedCollection | finance | `11-codegen/state-machines.yaml` |
| 18 | DunningCase | finance | `11-codegen/state-machines.yaml` |
| 19 | ResourceTransfer | agreement | `11-codegen/state-machines.yaml` |
| 20 | TaxRule | tax | `11-codegen/state-machines.yaml` |
| 21 | Payable | ap | `11-codegen/state-machines.yaml` |
| 22 | OwnerSettlementBatch | owner-settlement | `11-codegen/state-machines.yaml` |
| 23 | BillingPlan | billing | `11-codegen/state-machines.yaml` |
| 24 | UtilityUsagePeriod | billing | `11-codegen/state-machines.yaml` |
| 25 | MeterReading | utility-property-parking | `11-codegen/state-machines.yaml` |
| 26 | ParkingVehicleBinding | utility-property-parking | `11-codegen/state-machines.yaml` |
| 27 | OperationWorkOrder | operations | `11-codegen/state-machines.yaml` |
| 28 | RenovationOrder | operations | `11-codegen/state-machines.yaml` |
| 29 | IntegrationTask | platform | `11-codegen/state-machines.yaml` |
| 30 | TenantMembership | tenant | `11-codegen/state-machines.yaml` |
| 31 | Customer | crm | `11-codegen/state-machines.yaml` |
| 32 | HandoverOrder | operations | `11-codegen/state-machines.yaml` |
| 33 | NotificationMessage | notification | `11-codegen/state-machines.yaml` |
| 34 | NotificationDelivery | notification | `11-codegen/state-machines.yaml` |

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
| `PAYING` | `verifiedCallback` | `SUCCESS` |
| `PAYING` | `failFinal` | `FAILED` |
| `CREATED` | `close` | `CLOSED` |
| `PAYING` | `close` | `CLOSED` |
| `CLOSED` | `recordLateSuccess` | `SUCCESS` |
| `SUCCESS` | `partialRefund` | `PARTIALLY_REFUNDED` |
| `PARTIALLY_REFUNDED` | `partialRefund` | `PARTIALLY_REFUNDED` |
| `SUCCESS` | `fullRefund` | `REFUNDED` |
| `PARTIALLY_REFUNDED` | `fullRefund` | `REFUNDED` |

Notes: no order-level UNKNOWN (uncertainty lives on PaymentAttempt/RefundOrder; read model reports `processingState=UNKNOWN` while order stays PAYING). `CLOSED -> SUCCESS` only via dedicated `recordLateSuccess` (MASTER-SPEC §17.6). SUCCESS/PARTIALLY_REFUNDED/REFUNDED never create new payment attempts.

## PaymentAttempt

Initial: `INITIATED`

| Current | Command/Event | Target |
|---|---|---|
| `INITIATED` | `prepayCreated` | `PREPAY_CREATED` |
| `INITIATED` | `providerFail` | `FAILED` |
| `INITIATED` | `uncertain` | `UNKNOWN` |
| `INITIATED` | `expire` | `EXPIRED` |
| `INITIATED` | `close` | `CLOSED` |
| `PREPAY_CREATED` | `clientLaunched` | `USER_PAYING` |
| `PREPAY_CREATED` | `providerFail` | `FAILED` |
| `PREPAY_CREATED` | `uncertain` | `UNKNOWN` |
| `PREPAY_CREATED` | `expire` | `EXPIRED` |
| `PREPAY_CREATED` | `close` | `CLOSED` |
| `USER_PAYING` | `paidCallback` | `SUCCESS` |
| `USER_PAYING` | `querySuccess` | `SUCCESS` |
| `USER_PAYING` | `queryFail` | `FAILED` |
| `USER_PAYING` | `queryUncertain` | `UNKNOWN` |
| `USER_PAYING` | `expire` | `EXPIRED` |
| `USER_PAYING` | `close` | `CLOSED` |
| `UNKNOWN` | `callbackSuccess` | `SUCCESS` |
| `UNKNOWN` | `querySuccess` | `SUCCESS` |
| `UNKNOWN` | `queryFail` | `FAILED` |
| `UNKNOWN` | `queryClosed` | `CLOSED` |
| `UNKNOWN` | `queryUncertain` | `UNKNOWN` |

Notes: at most one active `PREPAY_CREATED/USER_PAYING/UNKNOWN` attempt per PaymentOrder; UNKNOWN blocks new attempts and channel switching.

## RefundOrder

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `PENDING_APPROVAL` |
| `DRAFT` | `submitPreApproved` | `APPROVED` |
| `PENDING_APPROVAL` | `approve` | `APPROVED` |
| `PENDING_APPROVAL` | `reject` | `CANCELLED` |
| `APPROVED` | `sendProvider` | `PROCESSING` |
| `PROCESSING` | `providerSuccess` | `SUCCESS` |
| `PROCESSING` | `providerFail` | `FAILED` |
| `PROCESSING` | `providerUncertain` | `UNKNOWN` |
| `UNKNOWN` | `callbackSuccess` | `SUCCESS` |
| `UNKNOWN` | `querySuccess` | `SUCCESS` |
| `UNKNOWN` | `queryFail` | `FAILED` |
| `UNKNOWN` | `queryUncertain` | `UNKNOWN` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `APPROVED` | `cancel` | `CANCELLED` |

Notes: UNKNOWN never releases the Finance refund reservation; FAILED/CANCELLED release it idempotently; SUCCESS confirms it exactly once and is terminal (corrections are separate accounting operations).

## InvoiceApplication

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `SUBMITTED` |
| `SUBMITTED` | `approve` | `APPROVED` |
| `APPROVED` | `requestProvider` | `ISSUING` |
| `ISSUING` | `providerSuccess` | `ISSUED` |
| `ISSUING` | `providerFail` | `FAILED` |
| `ISSUING` | `providerUncertain` | `UNKNOWN` |
| `UNKNOWN` | `querySuccess` | `ISSUED` |
| `UNKNOWN` | `queryFail` | `FAILED` |

## RedFlushApplication

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `RED_FLUSHING` |
| `RED_FLUSHING` | `providerSuccess` | `RED_FLUSHED` |
| `RED_FLUSHING` | `providerFail` | `FAILED` |
| `RED_FLUSHING` | `providerUncertain` | `UNKNOWN` |
| `UNKNOWN` | `querySuccess` | `RED_FLUSHED` |
| `UNKNOWN` | `queryFail` | `FAILED` |

Notes: standalone aggregate (`invoice_red_flush_application`); red flush creates a new red invoice relation, the original Invoice stays immutable and `ISSUED`.

## InvoiceDeliveryInstruction

Initial: `CREATED`

| Current | Command/Event | Target |
|---|---|---|
| `CREATED` | `queue` | `QUEUED` |
| `QUEUED` | `start` | `SENDING` |
| `SENDING` | `markSent` | `SENT` |
| `SENDING` | `markPartiallySent` | `PARTIALLY_SENT` |
| `CREATED` | `failFinal` | `FAILED` |
| `QUEUED` | `failFinal` | `FAILED` |
| `SENDING` | `failFinal` | `FAILED` |
| `CREATED` | `cancel` | `CANCELLED` |
| `QUEUED` | `cancel` | `CANCELLED` |

Notes: independent from InvoiceApplication; email failure never reverts `Invoice.status=ISSUED`.

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

## DunningCase

Initial: `OPEN`

| Current | Command/Event | Target |
|---|---|---|
| `OPEN` | `promiseToPay` | `PROMISED` |
| `PROMISED` | `markKept` | `KEPT` |
| `PROMISED` | `markBroken` | `BROKEN` |
| `BROKEN` | `reopen` | `OPEN` |
| `OPEN` | `close` | `CLOSED` |
| `PROMISED` | `close` | `CLOSED` |
| `KEPT` | `close` | `CLOSED` |
| `BROKEN` | `close` | `CLOSED` |

Notes: escalation levels (D+1/D+3/D+7/D+15/D+30...) are an `escalationLevel` field, never states; `close` covers settled-out and bad-debt closure.

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
| `DRAFT` | `cancel` | `CANCELLED` |
| `QUOTED` | `cancel` | `CANCELLED` |
| `TARGET_HELD` | `cancel` | `CANCELLED` |
| `PENDING_APPROVAL` | `cancel` | `CANCELLED` |
| `APPROVED` | `cancel` | `CANCELLED` |

Notes: cancellation is allowed only before supplementary signing; post-scheduling aborts go through the persisted saga/compensation path.

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
| `OVERDUE` | `settle` | `PAID` |
| `OPEN` | `cancel` | `CANCELLED` |
| `OPEN` | `writeOff` | `WRITTEN_OFF` |
| `OVERDUE` | `writeOff` | `WRITTEN_OFF` |

Notes: `cancel` only before irreversible financial effects.

## OwnerSettlementBatch

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `calculate` | `CALCULATED` |
| `CALCULATED` | `review` | `REVIEWING` |
| `REVIEWING` | `approve` | `APPROVED` |
| `REVIEWING` | `reject` | `REJECTED` |
| `REJECTED` | `revise` | `DRAFT` |
| `APPROVED` | `createPayable` | `PAYABLE_CREATED` |
| `APPROVED` | `cancel` | `CANCELLED` |
| `PAYABLE_CREATED` | `pay` | `PAYING` |
| `PAYING` | `paid` | `PAID` |
| `PAYING` | `uncertain` | `UNKNOWN` |
| `UNKNOWN` | `querySuccess` | `PAID` |
| `UNKNOWN` | `queryFail` | `FAILED` |
| `PAID` | `close` | `CLOSED` |

Notes: `APPROVED -> CANCELLED` only before payable creation, with audit. CLOSED batches are never edited (architecture red line).

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

## MeterReading

Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `submit` | `SUBMITTED` |
| `SUBMITTED` | `autoValidateOk` | `VERIFIED` |
| `SUBMITTED` | `anomalyFound` | `REVIEW_REQUIRED` |
| `REVIEW_REQUIRED` | `approve` | `VERIFIED` |
| `REVIEW_REQUIRED` | `reject` | `REJECTED` |
| `VERIFIED` | `markBilled` | `BILLED` |
| `VERIFIED` | `correct` | `CORRECTED` |
| `BILLED` | `correct` | `CORRECTED` |

Notes: `BILLED` is historical fact; correction closes the old version as `CORRECTED` and creates a new `DRAFT` version; only VERIFIED/BILLED readings drive billing.

## ParkingVehicleBinding

Initial: `PENDING`

| Current | Command/Event | Target |
|---|---|---|
| `PENDING` | `activate` | `ACTIVE` |
| `ACTIVE` | `end` | `ENDED` |
| `ACTIVE` | `cancel` | `CANCELLED` |

Notes: changing vehicle = end old ACTIVE binding + create a new binding; no in-place overwrite of historical plate association.

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

## NotificationMessage

Initial: `CREATED`

| Current | Command/Event | Target |
|---|---|---|
| `CREATED` | `queue` | `QUEUED` |
| `QUEUED` | `start` | `PROCESSING` |
| `PROCESSING` | `completeAll` | `COMPLETED` |
| `PROCESSING` | `completePartial` | `PARTIALLY_COMPLETED` |
| `PROCESSING` | `failFinal` | `FAILED` |
| `CREATED` | `cancel` | `CANCELLED` |
| `QUEUED` | `cancel` | `CANCELLED` |

## NotificationDelivery

Initial: `PENDING`

| Current | Command/Event | Target |
|---|---|---|
| `PENDING` | `send` | `SENDING` |
| `SENDING` | `providerAccepted` | `SENT` |
| `SENDING` | `tempFail` | `RETRY_WAIT` |
| `SENDING` | `finalFail` | `FAILED` |
| `SENDING` | `suppress` | `SUPPRESSED` |
| `RETRY_WAIT` | `retry` | `SENDING` |
| `SENT` | `deliveryReceipt` | `DELIVERED` |
| `SENT` | `bounce` | `BOUNCED` |
| `SENT` | `providerReject` | `REJECTED` |

Notes: uncertain provider results stay in `SENDING` with `result_uncertain` flag; never immediately send a duplicate.
