# Owner Settlement Domain SPEC

## Scope
For managed-property models where the tenant operates external owners' assets and settles eligible revenue to them.

## Models
PropertyOwner, OwnerOperatingAgreement, SettlementRule, OwnerSettlementBatch,
OwnerSettlementItem, OwnerStatement, OwnerPayable, OwnerPayout.

## Calculation
eligible allocated rent/service revenue
- management commission
- property-management deductions
- maintenance deductions
- taxes/withholding
- approved deductions
= owner payable.

Every line references a source business record.

## Settlement Rule
fixed percentage, fixed management fee, tiered percentage, minimum guarantee,
fixed owner rent, revenue share, expense pass-through, version/effective dates.

## Closed Loop
eligible business facts -> settlement candidate -> batch -> calculation -> statement ->
review/approval -> OwnerPayable in AP -> payout -> ledger -> reconciliation -> CLOSED.

## Invariants
- same source cannot be consumed twice
- rule version is snapshotted
- approved statement required before payable
- closed batches immutable
- post-close change uses adjustment batch
- tenant and owner permissions mandatory

## APIs
/api/admin/v1/property-owners
/api/admin/v1/owner-operating-agreements
/api/admin/v1/owner-settlements
/api/admin/v1/owner-statements
