# Agreement Party SPEC

## Goal
Enterprise agreements must explicitly model legal/commercial parties rather than assuming `customer_id` is sufficient.

Roles: LESSOR, LESSEE, PAYER, USER, GUARANTOR, OWNER, OPERATOR, INVOICE_PARTY, BROKER.

## Model
`agreement_party` is effective-dated and snapshots legal identity at signing.

Core fields:
- id, tenant_id, agreement_id
- party_role, party_type
- customer_id / organization_id / external_party_ref
- legal_name_snapshot, unified_credit_code_snapshot
- id_number_masked_snapshot
- contact_name_snapshot, phone_masked_snapshot, email_masked_snapshot
- address_snapshot
- bank_name_snapshot, bank_account_masked_snapshot
- tax_no_snapshot
- effective_from, effective_to
- status
- source_type, source_id
- version + audit columns

## Invariants
1. Every LEASE agreement has exactly one active LESSEE.
2. PAYER may differ from LESSEE.
3. INVOICE_PARTY may differ from PAYER.
4. Signed party snapshots are immutable.
5. Signed/effective changes require `AgreementChange(PARTY_CHANGE)`.
6. Historical rows are never deleted.
7. Party IDs must resolve inside the same tenant.
8. Payer/invoice-party changes trigger downstream billing/invoice re-validation.

## Commands
- AddAgreementPartyCommand
- RemoveAgreementPartyCommand (draft only)
- ChangeAgreementPartyCommand
- GetAgreementPartiesQuery

## APIs
- GET /api/admin/v1/agreements/{id}/parties
- POST /api/admin/v1/agreements/{id}/parties
- POST /api/admin/v1/agreements/{id}/party-changes

## Event
`agreement.agreement.party-changed.v1`

## Permissions
- agreement:party:view
- agreement:party:manage
- agreement:party:change

## Tests
- lessee differs from payer
- payer differs from invoice party
- signed party cannot be edited
- party change keeps history
- cross-tenant customer injection rejected
