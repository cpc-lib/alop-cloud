# INVOICE DOMAIN SPEC

## 1. Bounded Context / Service
`alop-invoice`

## 2. Aggregate Roots
- `InvoiceApplication`
- `Invoice`
- `RedFlushApplication`

## 3. Owned Tables
- `invoice_application`
- `invoice_application_item`
- `invoice`
- `invoice_item`
- `invoice_relation`
- `invoice_red_flush_application`

## 4. Commands
- `CreateInvoiceApplication`
- `ApproveInvoiceApplication`
- `IssueInvoice`
- `QueryUnknownInvoice`
- `RequestRedFlush`
- `QueryUnknownRedFlush`

## 5. Queries
- `GetInvoice`
- `ListInvoiceApplications`

## 6. Produced Events
- `InvoiceIssued`
- `InvoiceRedFlushed`

## 7. Permissions
- `invoice:apply`
- `invoice:approve`
- `invoice:red-flush`

## 8. Invariants
- `invoice amount <= reserved quota`
- `UNKNOWN never blindly retried`
- `red flush creates new red invoice relation, original invoice immutable`
- `reissue requires new application`

## 9. Transaction / Locking
- `Finance quota reservation handles amount concurrency`

## 10. Idempotency
- `providerRequestNo unique`
- `applicationNo unique`

## 11. Closure Condition
Invoice closed when all applications/provider requests have final state and quota/relations reflect issued or red-flushed facts.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.

## 16. V6.4 Email Delivery
Invoice email delivery is defined in `EMAIL-DELIVERY-SPEC.md`.

Additional owned tables:
- `invoice_delivery_instruction`
- `invoice_delivery_recipient`

Additional commands:
- `CreateInvoiceEmailDelivery`
- `RequestInvoiceEmailResend`
- `ApplyNotificationDeliveryResult`

Additional produced event:
- `invoice.invoice.delivery-requested.v1`

Additional consumed events:
- `notification.delivery.sent.v1`
- `notification.delivery.failed.v1`
- `notification.delivery.bounced.v1`

Invariants:
- Invoice issuance success and email delivery success are separate facts.
- Email failure never reverts `Invoice.status=ISSUED`.
- Auto email send is deduplicated; manual resend creates immutable new instruction/history.
- Email addresses are encrypted at rest and masked in API/logs.
- PDF/OFD bytes never enter RabbitMQ.
