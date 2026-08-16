# TASK-025 — Notification Center + Invoice Email Delivery

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.


## Business Goal
Implement multi-tenant reminder center with SMS/EMAIL/IN_APP and issued-invoice email delivery/retry/history.

## Scope
`alop-notification + alop-invoice + alop-file` integration.

## Must Read
- `00-master/MASTER-SPEC-V7.0.md`
- `02-domain/notification/*`
- `02-domain/invoice/EMAIL-DELIVERY-SPEC.md`
- notification/invoice Flyway
- notification + invoice OpenAPI
- Event schemas
- error/permission registries
- `08-tests/notification-invoice-email.md`

## Required Java domain model
Notification:
- `NotificationRule`
- `NotificationTemplate`
- `NotificationMessage`
- `NotificationDelivery`
- `RecipientPreference`
- `SmsProvider`
- `EmailProvider`

Invoice:
- `InvoiceDeliveryInstruction`
- `InvoiceDeliveryRecipient`

## Mandatory use cases
- event-driven notification generation;
- time-trigger reminder event handling;
- SMS send/retry/receipt;
- Email send/retry/bounce;
- invoice auto email after ISSUED;
- invoice manual resend;
- secure PDF attachment loading by FileId;
- provider configuration per tenant;
- delivery history query;
- IntegrationTask after terminal failure.
- NotificationRetryJob / delivery worker recovery.
- Agreement/Receivable/Reservation/WorkOrder trigger event integration.

## Transaction / consistency
- Business event consumption + NotificationMessage/Delivery + Inbox committed locally.
- Provider call happens outside long DB transaction using claimed delivery worker.
- Provider result persisted idempotently.
- Invoice auto delivery instruction + Outbox committed in invoice local transaction after issued file metadata is ready.
- Email failure never changes Invoice ISSUED status.

## Security
- no PII email/phone in MQ payload;
- encrypted address + HMAC hash;
- provider secret only through SecretManager reference;
- FileService internal authenticated fetch;
- tenant isolation fail closed.

## Tests
Implement every case in `08-tests/notification-invoice-email.md` using JUnit5/Testcontainers where applicable.

## Definition of Done
Compile + Flyway + OpenAPI + Event schema + domain/integration/tenant tests + provider mock tests + retry worker + metrics + runbook + SPEC mapping; no TODO.
