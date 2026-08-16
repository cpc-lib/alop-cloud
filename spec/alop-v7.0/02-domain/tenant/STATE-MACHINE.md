# State Machine

| Current | Event | Target |
|---|---|---|
| CREATING | PROVISION_SUCCESS | ACTIVE |
| ACTIVE | SUSPEND | SUSPENDED |
| SUSPENDED | RESUME | ACTIVE |
| ACTIVE | EXPIRE | EXPIRED |
| ACTIVE | TERMINATE_REQUEST | TERMINATING |
| SUSPENDED | TERMINATE_REQUEST | TERMINATING |
| EXPIRED | TERMINATE_REQUEST | TERMINATING |
| TERMINATING | FINALIZE | TERMINATED |

## Rules
- Only explicit Command methods may move state.
- Generic update API may not set status.
- Terminal states may not be reopened except by an explicit domain command documented in this SPEC.