# NOTIFICATION STATE MACHINE

## NotificationMessage
| Current | Command | Guard | Target |
|---|---|---|---|
| CREATED | QUEUE | recipient+template resolved | QUEUED |
| QUEUED | START | at least one delivery pending | PROCESSING |
| PROCESSING | ALL_REQUIRED_FINAL_SUCCESS | required deliveries satisfied | COMPLETED |
| PROCESSING | SOME_SUCCESS_SOME_FINAL_FAILED | no more fallback/retry | PARTIALLY_COMPLETED |
| PROCESSING | ALL_FINAL_FAILED | no more retry/fallback | FAILED |
| CREATED/QUEUED | CANCEL | no delivery already SENT | CANCELLED |

## NotificationDelivery
| Current | Command | Target |
|---|---|---|
| PENDING | SEND | SENDING |
| SENDING | PROVIDER_ACCEPTED | SENT |
| SENDING | TEMP_FAILURE | RETRY_WAIT |
| SENDING | FINAL_FAILURE | FAILED |
| SENDING | SUPPRESS | SUPPRESSED |
| RETRY_WAIT | RETRY | SENDING |
| SENT | DELIVERY_RECEIPT | DELIVERED |
| SENT | EMAIL_BOUNCE | BOUNCED |
| SENT | PROVIDER_REJECT | REJECTED |

UNKNOWN provider result remains `SENDING` with `result_uncertain=1` or transitions to a provider-specific `UNKNOWN` handling path; never immediately send a duplicate.
