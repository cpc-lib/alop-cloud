# State Machine

| Current | Event | Target |
|---|---|---|
| PENDING | HOLD | HELD |
| HELD | CONFIRM | CONFIRMED |
| HELD | EXPIRE | EXPIRED |
| HELD | CANCEL | CANCELLED |
| CONFIRMED | CONVERT | CONVERTED |
| CONFIRMED | CANCEL | CANCELLED |

## Rules
- Only explicit Command methods may move state.
- Generic update API may not set status.
- Terminal states may not be reopened except by an explicit domain command documented in this SPEC.