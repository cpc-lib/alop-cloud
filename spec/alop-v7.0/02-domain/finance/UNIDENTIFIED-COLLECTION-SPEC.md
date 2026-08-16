# Unidentified Collection SPEC

## Goal
Handle real money received when payer/customer/agreement cannot yet be safely identified.

## Aggregate
`UnidentifiedCollection`

Status:
OPEN, MATCH_CANDIDATE, CLAIM_PENDING_APPROVAL, CLAIMED, REFUND_PENDING, REFUNDED, CLOSED.

Core fields:
source_type/source_id, bank_account_id, provider_reference, payer_name_raw,
payer_account_masked, currency, amount, received_at, memo_raw, normalized_memo,
claimed_customer_id, claimed_agreement_id, resulting_collection_id,
claimed_by, claimed_at, claim_reason, version.

## Flow
Statement item
-> deterministic match fails
-> create UnidentifiedCollection
-> matching engine proposes candidates
-> finance review
-> optional approval
-> CLAIMED
-> create normal CollectionRecord
-> Allocation or CustomerAdvance
-> Ledger
-> CLOSED.

## Matching signals
payment/agreement number, payer name, account fingerprint, amount, memo tokens,
expected receivable, historical payer relationship.

Suggestions are not final truth. Auto-claim is allowed only above an explicit threshold.

## Invariants
- one statement item maps to at most one active unidentified record
- CLAIMED creates exactly one CollectionRecord
- amount/currency cannot be edited
- correction uses reversal + re-claim
- cross-tenant claims are forbidden

## APIs
GET /api/admin/v1/finance/unidentified-collections
POST /api/admin/v1/finance/unidentified-collections/{id}/claim
POST /api/admin/v1/finance/unidentified-collections/{id}/refund
POST /api/admin/v1/finance/unidentified-collections/{id}/reopen
