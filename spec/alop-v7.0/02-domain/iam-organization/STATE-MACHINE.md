# IAM-ORGANIZATION State Machines — V7.0

> All transitions are command-driven. Generic `status` update endpoints are forbidden.

## TenantMembership
Initial: `ACTIVE`

| Current | Command/Event | Target |
|---|---|---|
| `ACTIVE` | `suspend` | `SUSPENDED` |
| `SUSPENDED` | `resume` | `ACTIVE` |
| `ACTIVE` | `leave` | `LEFT` |
| `SUSPENDED` | `leave` | `LEFT` |

Invalid transitions MUST return a stable domain error and MUST NOT persist partial state.

