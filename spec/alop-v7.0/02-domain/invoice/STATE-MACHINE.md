# State Machine

| Current | Event | Target |
|---|---|---|
| DRAFT | SUBMIT | SUBMITTED |
| SUBMITTED | APPROVE | APPROVED |
| APPROVED | ISSUE_REQUEST | ISSUING |
| ISSUING | PROVIDER_SUCCESS | ISSUED |
| ISSUING | PROVIDER_FAIL | FAILED |
| ISSUING | PROVIDER_UNKNOWN | UNKNOWN |
| UNKNOWN | QUERY_SUCCESS | ISSUED |
| UNKNOWN | QUERY_FAIL | FAILED |
| ISSUED | RED_FLUSH_REQUEST | RED_FLUSHING |
| RED_FLUSHING | RED_SUCCESS | RED_FLUSHED |

## Rules
- Only explicit Command methods may move state.
- Generic update API may not set status.
- Terminal states may not be reopened except by an explicit domain command documented in this SPEC.
## InvoiceDeliveryInstruction V6.4
| Current | Action | Target |
|---|---|---|
| CREATED | QUEUE_NOTIFICATION | QUEUED |
| QUEUED | NOTIFICATION_STARTED | SENDING |
| SENDING | ALL_REQUIRED_EMAILS_SENT | SENT |
| SENDING | SOME_EMAILS_SENT | PARTIALLY_SENT |
| CREATED/QUEUED/SENDING | FINAL_FAILURE | FAILED |
| CREATED/QUEUED | CANCEL | CANCELLED |

`Invoice ISSUED` is independent from this state machine.
