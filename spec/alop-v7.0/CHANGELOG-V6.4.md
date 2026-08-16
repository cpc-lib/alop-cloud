# CHANGELOG V6.4

## Added
- Formal `alop-notification` Domain.
- SMS + EMAIL + IN_APP notification channels.
- NotificationRule, Template, Message, Delivery, Attempt, Preference, ProviderConfig, Suppression models.
- Tenant-level/platform-level SMS/Email provider configuration with SecretManager references.
- Reminder matrix for agreement expiry/renewal, bills, overdue, Reservation, payment/refund, work order and invoice.
- Quiet hours, fallback, deduplication, retry, provider receipt and bounce semantics.
- Invoice email auto delivery after `ISSUED`.
- Invoice email manual resend with immutable history.
- PDF/OFD secure attachment flow through File Service.
- InvoiceDeliveryInstruction / Recipient tables and OpenAPI.
- Notification OpenAPI, Flyway, events, tests, runbook and TASK-025.

## Architecture decisions
- Business services produce business triggers; Notification owns communication delivery.
- Invoice tax fact and email delivery fact are separate.
- Raw recipient PII and invoice files never enter RabbitMQ event payload.
