# ADR-014 — Central Notification Center and Invoice Email Delivery

## Status
Accepted in V6.4.

## Context
Agreement, Finance, CRM, Operations and Invoice all need customer/internal reminders. Direct SMS/SMTP integration inside each service would duplicate provider logic, leak credentials, break retry/dedup semantics and make tenant configuration inconsistent.

## Decision
Create formal `alop-notification` Bounded Context.

Business domains own **when** a notification becomes relevant and publish business triggers. Notification owns **how** it is delivered: recipient resolution, channel, template, provider, preferences, quiet hours, dedup, retry, fallback, receipt, suppression and audit.

Invoice email is modeled as:
`Invoice ISSUED + electronic file ready -> InvoiceDeliveryInstruction -> Outbox -> Notification -> EmailProvider -> delivery result`.

## Consequences
- invoice tax state is independent from email status;
- no raw email/phone or PDF bytes in RabbitMQ events;
- notification delivery is eventual consistency;
- duplicate business events are harmless through dedup keys;
- provider secrets are centralized behind SecretManager refs;
- tenant-specific provider config is supported;
- transaction/legal terminal failures become visible IntegrationTasks.
