# OPERATIONS State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

## HandoverOrder
Initial: `DRAFT`

| Current | Command/Event | Target |
|---|---|---|
| `DRAFT` | `start` | `IN_PROGRESS` |
| `IN_PROGRESS` | `submitConfirm` | `PENDING_CONFIRMATION` |
| `PENDING_CONFIRMATION` | `confirm` | `COMPLETED` |
| `DRAFT` | `cancel` | `CANCELLED` |
| `IN_PROGRESS` | `cancelBeforeFinal` | `CANCELLED` |

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

