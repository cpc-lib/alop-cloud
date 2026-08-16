# ASSET State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

