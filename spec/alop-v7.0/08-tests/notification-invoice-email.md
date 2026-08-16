# NOTIFICATION + INVOICE EMAIL TEST SPEC

## 1. Notification rule tests
- Agreement T-90 emits exactly one logical message for same triggerKey despite duplicate event/job.
- Bill due reminder can generate SMS+EMAIL parallel deliveries.
- Marketing opt-out suppresses MARKETING SMS/EMAIL.
- Transactional invoice email is not suppressed by marketing opt-out.
- Quiet hours postpone configured reminders according to Tenant timezone.
- LEGAL rule may bypass quiet hours only when explicitly configured.

## 2. Multi-tenant isolation
- Tenant A rule/template/provider cannot be used by Tenant B.
- Tenant A delivery query never returns Tenant B messages.
- forged tenant header fails closed.
- provider webhook resolves tenant through stored trusted provider config, not payload tenantId.

## 3. SMS tests
- valid template variables -> accepted.
- missing variable -> TEMPLATE_VARIABLE_MISSING, no provider call.
- duplicate delivery request -> one SMS only.
- provider 5xx -> retry schedule.
- invalid phone -> non-retryable failure.
- delivery receipt duplicate -> one state transition.

## 4. Email tests
- HTML + text fallback rendering.
- TO/CC/BCC handling.
- masked recipient in logs/API.
- bounce marks BOUNCED and optionally suppression.
- provider accepted without delivery receipt stays SENT, not fake DELIVERED.
- attachment too large follows tenant fallback policy.

## 5. Invoice auto email E2E
`Invoice ISSUED -> PDF fileId ready -> InvoiceDeliveryInstruction -> event -> Notification -> secure FileService fetch -> EmailProvider -> SENT -> delivery result -> instruction SENT`.

Assertions:
- Invoice stays ISSUED even if email fails.
- duplicate InvoiceIssued/delivery-requested events produce one auto email.
- event contains instructionId, not email or PDF bytes.
- PDF belongs to same tenant/invoice.

## 6. Manual resend
- first auto send success.
- operator manually resends twice.
- there are three immutable instructions/history records.
- manual resend requires `invoice:email:send` and Idempotency-Key.
- same idempotency key + same request returns first response.
- same idempotency key + changed recipient returns IDEMPOTENCY_KEY_CONFLICT.

## 7. Invoice recipient safety
- app customer cannot send invoice to arbitrary unverified email.
- admin arbitrary recipient requires permission and audit.
- email ciphertext is not exposed in raw API response.

## 8. Failure / chaos
- Notification service down: Invoice remains ISSUED; outbox/integration task preserves delivery request.
- Email provider down: retry then IntegrationTask.
- File service down: retry email, do not issue second invoice.
- RabbitMQ duplicate: no duplicate auto email.
- RabbitMQ delayed delivery after manual resend: auto dedup still holds.

## 9. Performance
- batch reminder generation paginated/keyset.
- provider worker pools isolated by channel.
- one noisy tenant cannot starve other tenants; tenant/provider rate limits tested.
