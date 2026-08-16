# Tax Rule State Machine

DRAFT -> PENDING_APPROVAL -> ACTIVE -> EXPIRED
PENDING_APPROVAL -> DRAFT on rejection
ACTIVE -> SUPERSEDED when a new effective version takes over
DRAFT -> CANCELLED

ACTIVE/SUPERSEDED rules are immutable.
