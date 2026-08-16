# FINANCE State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

