# Agreement CLOSED Checklist
- Agreement state EXPIRED or TERMINATED.
- No pending AgreementChange.
- MOVE_OUT/Handover completed if required.
- All related Occupancy ended/released.
- Receivable outstanding = 0 or approved WRITTEN_OFF.
- Deposit fully refunded/allocated/legally retained with records.
- No payment/refund UNKNOWN or PROCESSING.
- No invoice/red-flush UNKNOWN/PROCESSING; quota has final state.
- No unresolved CRITICAL reconciliation exception.
- No active sign/compensation Saga.
Only then may `CloseAgreementCommand` transition to CLOSED.
