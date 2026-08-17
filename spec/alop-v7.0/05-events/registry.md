# Event Registry (V7.0)

Canonical source: `05-events/event-registry.yaml`. This document is the derived human-readable view; every row below matches the registry entry one-to-one (43 events). The generated producer/consumer view for code generation is `11-codegen/EVENT-PRODUCER-CONSUMER-MATRIX.yaml` (produced by `scripts/generate_event_matrix.py`).

| Event Type | Producer | Consumers | Schema |
|---|---|---|---|
| agreement.agreement.closed.v1 | alop-agreement | alop-payment, alop-finance, alop-billing, alop-search | agreement-closed-v1.schema.json |
| agreement.agreement.effective.v1 | alop-agreement | alop-billing, alop-finance, alop-operations | agreement-effective-v1.schema.json |
| agreement.agreement.expiring.v1 | alop-agreement | alop-crm, alop-notification | agreement-expiring-v1.schema.json |
| agreement.agreement.party-changed.v1 | alop-agreement | (none registered) | agreement-party-changed-v1.schema.json |
| agreement.agreement.signed.v1 | alop-agreement | alop-billing, alop-crm, alop-notification | agreement-signed-v1.schema.json |
| agreement.renewal-priority.created.v1 | alop-agreement | alop-crm, alop-notification | renewal-priority-created-v1.schema.json |
| agreement.resource-transfer.requested.v1 | alop-agreement | alop-asset, alop-billing, alop-notification | resource-transfer-requested-v1.schema.json |
| agreement.resource-transfer.completed.v1 | alop-agreement | alop-billing, alop-crm, alop-notification | resource-transfer-completed-v1.schema.json |
| agreement.resource-transfer.failed.v1 | alop-agreement | alop-asset, alop-billing, alop-notification | resource-transfer-failed-v1.schema.json |
| ap.payable.created.v1 | alop-ap | alop-notification | ap-payable-created-v1.schema.json |
| asset.parking.vehicle-bound.v1 | alop-agreement | (none registered) | parking-vehicle-bound-v1.schema.json |
| asset.reservation.created.v1 | alop-reservation | alop-crm, alop-notification | reservation-created-v1.schema.json |
| asset.reservation.expired.v1 | alop-reservation | alop-crm, alop-notification | reservation-expired-v1.schema.json |
| asset.reservation.expiring.v1 | alop-reservation | alop-notification | reservation-expiring-v1.schema.json |
| asset.resource.sold.v1 | alop-asset | alop-agreement, alop-crm, alop-search | resource-sold-v1.schema.json |
| asset.utility-meter.reading-verified.v1 | alop-asset | alop-billing, alop-agreement | utility-meter-reading-verified-v1.schema.json |
| asset.utility-meter.reading-corrected.v1 | alop-asset | alop-billing, alop-finance | utility-meter-reading-corrected-v1.schema.json |
| billing.bill.issued.v1 | alop-billing | alop-finance, alop-notification | bill-issued-v1.schema.json |
| billing.property-fee.billed.v1 | alop-billing | alop-finance, alop-notification | property-fee-billed-v1.schema.json |
| billing.utility-charge.billed.v1 | alop-billing | alop-finance, alop-notification | utility-charge-billed-v1.schema.json |
| billing.utility-usage.calculated.v1 | alop-billing | (none registered) | utility-usage-calculated-v1.schema.json |
| billing.utility-usage.corrected.v1 | alop-billing | alop-finance, alop-notification | utility-usage-corrected-v1.schema.json |
| finance.collection.created.v1 | alop-finance | alop-agreement, alop-search, alop-notification | collection-created-v1.schema.json |
| finance.receivable.due-reminder.v1 | alop-finance | (none registered) | receivable-due-reminder-v1.schema.json |
| finance.receivable.overdue.v1 | alop-finance | alop-crm, alop-notification | receivable-overdue-v1.schema.json |
| finance.reconciliation.exception-created.v1 | alop-finance | alop-notification | reconciliation-exception-created-v1.schema.json |
| finance.security-deposit.settled.v1 | alop-finance | alop-agreement, alop-notification | security-deposit-settled-v1.schema.json |
| finance.unidentified-collection.claimed.v1 | alop-finance | alop-crm | unidentified-collection-claimed-v1.schema.json |
| invoice.invoice.delivery-requested.v1 | alop-invoice | alop-notification | invoice-delivery-requested-v1.schema.json |
| invoice.invoice.issued.v1 | alop-invoice | alop-notification, alop-finance, alop-crm | invoice-issued-v1.schema.json |
| invoice.invoice.red-flushed.v1 | alop-invoice | alop-finance, alop-notification | invoice-red-flushed-v1.schema.json |
| notification.delivery.sent.v1 | alop-notification | alop-invoice, alop-integration | notification-delivery-result-v1.schema.json |
| notification.delivery.failed.v1 | alop-notification | alop-invoice, alop-integration | notification-delivery-result-v1.schema.json |
| notification.delivery.bounced.v1 | alop-notification | alop-invoice, alop-integration | notification-delivery-result-v1.schema.json |
| operations.work-order.sla-violated.v1 | alop-operations | alop-notification | work-order-sla-violated-v1.schema.json |
| owner-settlement.statement.approved.v1 | alop-owner-settlement | alop-ap, alop-notification | owner-settlement-approved-v1.schema.json |
| payment.payment.closed.v1 | alop-payment | alop-crm, alop-reservation | payment-closed-v1.schema.json |
| payment.payment.created.v1 | alop-payment | alop-notification | payment-created-v1.schema.json |
| payment.payment.succeeded.v1 | alop-payment | alop-finance, alop-crm, alop-notification | payment-succeeded-v1.schema.json |
| payment.payment.unknown.v1 | alop-payment | alop-notification, alop-operations | payment-unknown-v1.schema.json |
| payment.refund.succeeded.v1 | alop-payment | alop-finance, alop-notification | payment-refunded-v1.schema.json |
| payment.refund.failed.v1 | alop-payment | (none registered) | refund-failed-v1.schema.json |
| tax.rule.published.v1 | alop-tax | alop-billing, alop-invoice | tax-rule-published-v1.schema.json |

Notes:
- `alop-reservation` is the standalone reservation service (ADR-023); it owns `asset.reservation.*` events.
- `asset.parking.vehicle-bound.v1` is produced by `alop-agreement` because `ParkingVehicleBinding` lives in the agreement database (02-domain/utility-property-parking/DOMAIN-SPEC.md).
- The three `notification.delivery.*` events share one schema (`notification-delivery-result-v1.schema.json`, discriminated by `eventType`).

## Payment Event Contract Rules

- `payment.payment.succeeded.v1` is emitted only from verified provider callback, verified provider query, or approved reconciliation repair evidence; client SDK success never emits it.
- `payment.payment.unknown.v1` is operational; it must not create Collection.
- `payment.refund.succeeded.v1` carries `refundReservationId` so Finance can idempotently confirm the exact reserved refundable amount.
- `payment.payment.closed.v1` covers unpaid payment order closure/expiry; reservation deposit scenario may consume it together with `asset.reservation.expired.v1`.
- Events always include trusted `tenantId`; callback-supplied tenant values are ignored.

## Notification / Invoice Email Rules

- `invoice.invoice.delivery-requested.v1` -> alop-notification: create EMAIL delivery from secure InvoiceDeliveryInstruction.
- `notification.delivery.sent.v1` -> source domain read model: record final/accepted send status.
- `notification.delivery.failed.v1` -> source domain read model + IntegrationTask when terminal.
- `notification.delivery.bounced.v1` -> invoice/CRM: update delivery history and recipient data-quality task.

Reminder source events (business contexts own trigger facts; Notification resolves recipients in tenant scope):
- `agreement.agreement.expiring.v1`
- `agreement.renewal-priority.created.v1`
- `billing.bill.issued.v1`
- `finance.receivable.due-reminder.v1`
- `finance.receivable.overdue.v1`
- `payment.payment.succeeded.v1`
- `payment.refund.succeeded.v1`
- `asset.reservation.expiring.v1`
- `operations.work-order.sla-violated.v1`

Business events do not carry raw email/phone.

## Naming Map (PascalCase -> canonical dotted event)

DOMAIN-SPECs describe produced events in PascalCase. The canonical cross-service event names are the dotted names below; "-" means the domain event is currently internal and NOT registered in `event-registry.yaml` (do not publish it without registering first).

| Domain | PascalCase event | Canonical dotted event |
|---|---|---|
| agreement | AgreementSigned | agreement.agreement.signed.v1 |
| agreement | AgreementEffective | agreement.agreement.effective.v1 |
| agreement | AgreementExpiring | agreement.agreement.expiring.v1 |
| agreement | AgreementClosed | agreement.agreement.closed.v1 |
| agreement | AgreementApproved | - |
| agreement | AgreementExpired | - |
| agreement | AgreementTerminated | - |
| agreement | AgreementPartyChanged | agreement.agreement.party-changed.v1 |
| agreement | RenewalPriorityCreated | agreement.renewal-priority.created.v1 |
| agreement | ResourceTransferRequested | agreement.resource-transfer.requested.v1 |
| agreement | ResourceTransferCompleted | agreement.resource-transfer.completed.v1 |
| agreement | ResourceTransferFailed | agreement.resource-transfer.failed.v1 |
| ap | PayableCreated | ap.payable.created.v1 |
| asset | ResourceSold | asset.resource.sold.v1 |
| asset | UtilityMeterReadingVerified | asset.utility-meter.reading-verified.v1 |
| asset | UtilityMeterReadingCorrected | asset.utility-meter.reading-corrected.v1 |
| asset | ParkingVehicleBound | asset.parking.vehicle-bound.v1 |
| asset | AssetApproved | - |
| asset | AssetValuated | - |
| asset | ListingPublished | - |
| asset | RenovationStarted | - |
| asset | MaintenanceStarted | - |
| billing | BillIssued | billing.bill.issued.v1 |
| billing | BillCancelled | - |
| billing | UtilityChargeBilled | billing.utility-charge.billed.v1 |
| billing | PropertyManagementFeeBilled | billing.property-fee.billed.v1 |
| billing | UtilityUsageCalculated | billing.utility-usage.calculated.v1 |
| billing | UtilityUsageCorrected | billing.utility-usage.corrected.v1 |
| crm | LeadCreated | - |
| crm | CustomerCreated | - |
| crm | OpportunityStageChanged | - |
| crm | ViewingCompleted | - |
| crm | QuotationSent | - |
| crm | QuotationAccepted | - |
| crm | QuotationExpired | - |
| finance | ReceivableCreated | - |
| finance | ReceivableOverdue | finance.receivable.overdue.v1 |
| finance | ReceivableDueReminder | finance.receivable.due-reminder.v1 |
| finance | CollectionCreated | finance.collection.created.v1 |
| finance | AllocationCreated | - |
| finance | AllocationReversed | - |
| finance | ReconciliationExceptionCreated | finance.reconciliation.exception-created.v1 |
| finance | SecurityDepositSettled | finance.security-deposit.settled.v1 |
| finance | UnidentifiedCollectionClaimed | finance.unidentified-collection.claimed.v1 |
| iam-organization | PermissionChanged | - |
| iam-organization | CustomerOwnerChanged | - |
| iam-organization | ResourceOwnerChanged | - |
| invoice | InvoiceIssued | invoice.invoice.issued.v1 |
| invoice | InvoiceRedFlushed | invoice.invoice.red-flushed.v1 |
| invoice | InvoiceDeliveryRequested | invoice.invoice.delivery-requested.v1 |
| notification | NotificationSent | notification.delivery.sent.v1 |
| notification | NotificationFailed | notification.delivery.failed.v1 |
| notification | NotificationBounced | notification.delivery.bounced.v1 |
| notification | NotificationQueued | - |
| notification | NotificationDelivered | - |
| notification | NotificationSuppressed | - |
| operations | WorkOrderSlaViolated | operations.work-order.sla-violated.v1 |
| operations | WorkOrderCreated | - |
| operations | WorkOrderClosed | - |
| operations | RenovationCompleted | - |
| owner-settlement | OwnerSettlementStatementApproved | owner-settlement.statement.approved.v1 |
| payment | PaymentCreated | payment.payment.created.v1 |
| payment | PaymentSucceeded | payment.payment.succeeded.v1 |
| payment | PaymentUnknown | payment.payment.unknown.v1 |
| payment | PaymentClosed | payment.payment.closed.v1 |
| payment | PaymentRefunded | payment.refund.succeeded.v1 |
| payment | PaymentRefundFailed | payment.refund.failed.v1 |
| platform-integration | IntegrationTaskCreated | - |
| reservation | ReservationCreated | asset.reservation.created.v1 |
| reservation | ReservationConfirmed | - |
| reservation | ReservationExpired | asset.reservation.expired.v1 |
| reservation | ReservationConverted | - |
| tax | TaxRulePublished | tax.rule.published.v1 |
| tenant | TenantCreated | tenant.tenant.created.v1 (V6 legacy name; NOT registered in V7.0) |
| tenant | TenantSuspended | - |
| tenant | TenantResumed | - |
| tenant | TenantTerminated | - |
| tenant | TenantRouteChanged | - |

Retired V6 names (must not be used in new contracts): `payment.payment.refunded.v1` -> use `payment.refund.succeeded.v1`; `notification-delivery-result-v1.schema` (schema filename used as eventType) -> use `notification.delivery.sent/failed/bounced.v1`.
