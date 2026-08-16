# PLATFORM-INTEGRATION State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

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

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

