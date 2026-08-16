# Observability
Structured logs: service, traceId, spanId, tenantId, userId, membershipId, supportSessionId, businessType, businessId, event.
Metrics: reservation_success/conflict, agreement_signed, receivable_outstanding, payment_success/unknown, invoice_unknown, reconciliation_exception, outbox_dead, integration_pending, db_deadlock.
Avoid tenantId as unbounded Prometheus label; VIP tenant labels only, detailed tenant metrics in business analytics store.


## V6.3 Metrics
- `utility_reading_submitted_total`
- `utility_reading_anomaly_total`
- `utility_reading_unbilled_verified`
- `utility_adjustment_total`
- `property_fee_bill_amount`
- `parking_reservation_conflict_total`
- `parking_active_occupancy`

## V6.4 Notification observability
Track notification queue/retry backlog, SMS provider rejects, email bounce rate, provider latency/error rate, invoice-email success/failure and terminal IntegrationTasks. Avoid tenantId as unbounded Prometheus label; detailed tenant breakdown belongs in operational read models/log search.
