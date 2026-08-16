# State Machine

| Current | Event | Target |
|---|---|---|
| DRAFT | SUBMIT | PENDING_APPROVAL |
| PENDING_APPROVAL | APPROVE | APPROVED |
| PENDING_APPROVAL | REJECT | DRAFT |
| APPROVED | START_SIGNATURE | WAITING_SIGNATURE |
| WAITING_SIGNATURE | SIGN | SIGNED |
| SIGNED | ACTIVATE | EFFECTIVE |
| EFFECTIVE | MARK_EXPIRING | EXPIRING |
| EXPIRING | EXPIRE | EXPIRED |
| EFFECTIVE | TERMINATE_REQUEST | TERMINATING |
| EXPIRING | TERMINATE_REQUEST | TERMINATING |
| TERMINATING | TERMINATE | TERMINATED |
| EXPIRED | CLOSE | CLOSED |
| TERMINATED | CLOSE | CLOSED |

## Rules
- Only explicit Command methods may move state.
- Generic update API may not set status.
- Terminal states may not be reopened except by an explicit domain command documented in this SPEC.