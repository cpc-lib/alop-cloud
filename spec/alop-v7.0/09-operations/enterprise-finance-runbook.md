# Enterprise Finance Runbook V6.5

## Security Deposit Incident
1. identify agreement/deposit account
2. verify liability balance and append-only transaction history
3. never update balance columns by SQL
4. use reversal/adjustment command
5. verify ledger and agreement close condition

## Unidentified Bank Receipt
1. inspect statement source and amount
2. run candidate matching
3. verify payer evidence
4. claim only inside same tenant
5. create Collection through application command
6. allocate or leave CustomerAdvance
7. record audit/operator/reason

## AP Payout UNKNOWN
1. do not retry payout blindly
2. query bank/provider by payout request number
3. resolve SUCCESS/FAILED
4. settle/release payable reservation only after final result

## Owner Settlement Dispute
1. freeze payout for affected batch
2. retain original calculation
3. create adjustment batch
4. never edit CLOSED batch
