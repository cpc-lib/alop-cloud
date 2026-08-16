# Event Registry

| Routing Key | Producer | Consumer(s) | Purpose |
|---|---|---|---|
| tenant.tenant.created.v1 | tenant | iam, organization | provision defaults |
| asset.reservation.created.v1 | asset | crm, notification | reservation timeline/notification |
| asset.reservation.expired.v1 | asset | crm, notification | release & follow-up |
| agreement.agreement.signed.v1 | agreement | billing, crm, notification | billing and timeline |
| agreement.agreement.effective.v1 | agreement | crm, notification | fulfillment start |
| billing.bill.issued.v1 | billing | finance, notification | create receivable |
| payment.payment.succeeded.v1 | payment | finance, crm | verified payment fact; finance creates Collection exactly once |
| payment.payment.unknown.v1 | payment | notification, operations | provider result uncertain; query required; do not retry payment blindly |
| payment.payment.closed.v1 | payment | crm, reservation | unpaid payment order closed/expired |
| payment.refund.succeeded.v1 | payment | finance, crm | confirm refund reservation, reverse financial effects and timeline |
| payment.refund.failed.v1 | payment | finance, operations | release refund reservation after definitive provider failure |
| finance.collection.created.v1 | finance | crm | timeline |
| finance.receivable.overdue.v1 | finance | crm, notification | dunning |
| invoice.invoice.issued.v1 | invoice | finance, crm | confirm quota/timeline |
| invoice.invoice.red-flushed.v1 | invoice | finance, crm | restore quota/timeline |
| finance.reconciliation.exception-created.v1 | finance | notification | ops alert |
| asset.resource.sold.v1 | asset | search, crm, agreement | offline listing and close future acquisition paths |
| asset.utility-meter.reading-verified.v1 | asset | billing, agreement | verified usage for utility billing / handover settlement |
| asset.utility-meter.reading-corrected.v1 | asset | billing, finance | generate billing adjustment when prior reading was already billed |
| asset.parking.vehicle-bound.v1 | agreement/asset | crm, notification | parking vehicle fulfillment timeline |
| billing.utility-charge.billed.v1 | billing | finance, crm | utility bill issued / receivable creation |
| billing.property-fee.billed.v1 | billing | finance, crm | property management fee bill issued / receivable creation |

## Payment Event Contract Rules — V6.3
- `PaymentSucceeded` is emitted only from verified provider callback, verified provider query, or approved reconciliation repair evidence.
- Client SDK success never emits `PaymentSucceeded`.
- `PaymentUnknown` is operational; it must not create Collection.
- `PaymentRefunded` contains `refundReservationId` so Finance can idempotently confirm the exact reserved refundable amount.
- Events always include trusted `tenantId`; callback-supplied tenant values are ignored.

## V6.4 Notification / Invoice Email
- `invoice.invoice.delivery-requested.v1` -> alop-notification: create EMAIL delivery from secure InvoiceDeliveryInstruction.
- `notification.delivery.sent.v1` -> source domain/read model: record final/accepted send status.
- `notification.delivery.failed.v1` -> source domain/read model + IntegrationTask when terminal.
- `notification.delivery.bounced.v1` -> invoice/CRM: update delivery history and recipient data-quality task.

Reminder source events/routing keys:
- `agreement.agreement.expiring.v1`
- `agreement.renewal-priority.created.v1`
- `billing.bill.issued.v1`
- `finance.receivable.due-reminder.v1`
- `finance.receivable.overdue.v1`
- `payment.payment.succeeded.v1`
- `payment.payment.refunded.v1`
- `asset.reservation.expiring.v1`
- `operations.work-order.sla-violated.v1`

Business events do not carry raw email/phone; Notification resolves recipients in tenant scope.

## V6.5 Enterprise Events
- `agreement.agreement.party-changed.v1`
- `agreement.resource-transfer.completed.v1`
- `finance.security-deposit.settled.v1`
- `finance.unidentified-collection.claimed.v1`
- `billing.utility-usage.calculated.v1`
- `tax.rule.published.v1`
- `ap.payable.created.v1`
- `owner-settlement.statement.approved.v1`
