# BILLING State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

## BillingPlan
Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `activate` | `ACTIVE` |
| `ACTIVE` | `complete` | `COMPLETED` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `ACTIVE` | `cancelFutureOnly` | `CANCELLED` |

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

