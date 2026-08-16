# V6.5 Enterprise Operations Hardening Test Plan

## Agreement Party
- lessee/payer/invoice party differ
- signed party immutable
- cross-tenant party injection rejected

## Security Deposit
- partial receipt/top-up
- damage + rent deduction
- concurrent refund reservations
- UNKNOWN refund retains reservation
- Agreement CLOSED blocked while deposit unsettled

## Utility Usage Period
- normal reading pair
- meter replacement mid-period
- reset/rollover anomaly
- estimated reading corrected after billing
- shared-meter allocation conserves total usage after rounding

## Unidentified Collection
- statement receipt cannot match
- low-confidence candidate does not auto-claim
- manual claim creates exactly one Collection
- reversal/re-claim retains audit
- cross-tenant claim forbidden

## Resource Transfer
- successful transfer
- target conflict
- target commit succeeds but agreement update fails -> compensation
- price/deposit increase/decrease
- source occupancy remains until target commit

## Tax
- exclusive/inclusive/non-taxable
- effective-date rate switch
- historical invoice keeps old snapshot
- multiple tax categories in one Bill

## AP
- duplicate supplier invoice
- partial payable payment
- payout UNKNOWN
- overpayment blocked
- ledger balanced

## Owner Settlement
- percentage share
- fixed guarantee
- maintenance deduction
- duplicate source consumption blocked
- adjustment after closed batch
- owner payout through AP
