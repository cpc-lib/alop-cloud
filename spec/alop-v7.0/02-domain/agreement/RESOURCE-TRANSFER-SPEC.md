# Resource Transfer / Change Room SPEC

## Goal
Changing a customer from one rented resource to another is a first-class business process.

Reasons: maintenance, upgrade/downgrade, operational relocation, resource issue, contract amendment.

## Aggregate
`ResourceTransfer`

Status:
DRAFT -> QUOTED -> TARGET_HELD -> PENDING_APPROVAL -> APPROVED ->
WAITING_SIGNATURE -> SCHEDULED -> EXECUTING -> COMPLETED.
Alternative: CANCELLED / FAILED.

Core fields:
transfer_no, agreement_id, customer_id, from_resource_unit_id, to_resource_unit_id,
requested_effective_at, actual_effective_at, reason_type/reason,
price_difference, deposit_difference, billing_impact_status,
workflow_instance_id, version.

## Flow
Create transfer
-> target eligibility
-> quote price/deposit difference
-> Reservation target
-> approval
-> supplementary signing
-> effective time:
target commit -> source MOVE_OUT -> target MOVE_IN -> old Occupancy ends ->
new Occupancy starts -> AgreementChange effective -> BillingRule new version ->
adjustment Bill/Receivable -> COMPLETED.

## Consistency
Target uses ScheduleGuard + ConflictGroup.
Execution is a persisted Saga across Agreement and Asset contexts.
Do not release source before target commit succeeds except explicit emergency policy.

## Invariants
from != to; target tenant-safe/conflict-free; agreement signed/effective;
old AgreementItem effective-dated, never deleted; all financial impacts traceable.

## Events
agreement.resource-transfer.requested.v1
agreement.resource-transfer.completed.v1
agreement.resource-transfer.failed.v1
