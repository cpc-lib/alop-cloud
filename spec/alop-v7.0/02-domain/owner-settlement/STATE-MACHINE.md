# Owner Settlement State Machine

DRAFT -> CALCULATED -> REVIEWING -> APPROVED -> PAYABLE_CREATED -> PAYING -> PAID -> CLOSED
REVIEWING -> REJECTED -> DRAFT
PAYING -> UNKNOWN -> PAID / FAILED
APPROVED -> CANCELLED only before payable creation and with audit.
