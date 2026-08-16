# CRM State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

## Lead
Initial: `NEW`

| Current | Command/Event | Target |
|---|---|---|
| `NEW` | `startContact` | `CONTACTING` |
| `CONTACTING` | `qualify` | `QUALIFIED` |
| `QUALIFIED` | `convert` | `CONVERTED` |
| `NEW` | `invalidate` | `INVALID` |
| `CONTACTING` | `invalidate` | `INVALID` |

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

