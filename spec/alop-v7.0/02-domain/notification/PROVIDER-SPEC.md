# NOTIFICATION PROVIDER SPEC

## Provider ports
`SmsProvider` and `EmailProvider` are Infrastructure ports. Provider result must be `ACCEPTED / REJECTED / UNKNOWN`, not boolean.

## Provider request idempotency
Each delivery has immutable `providerRequestNo`; retried transport requests reuse the same business delivery identity where provider idempotency permits. If provider requires a new request id, persist attempt-level request numbers and preserve parent Delivery.

## Email capabilities
Capability flags:
- html
- attachments
- deliveryReceipt
- bounceWebhook
- customSender
- replyTo
- maxAttachmentBytes

## SMS capabilities
Capability flags:
- templateMessage
- deliveryReceipt
- internationalNumber
- senderId
- statusQuery

## Callback/Webhook
Provider webhook processing:
1. read raw body;
2. verify signature before trusting payload;
3. resolve provider config / tenant from trusted provider identity;
4. find Delivery by provider message id / request no;
5. idempotently apply receipt;
6. write audit/outbox;
7. ACK quickly.

Never trust tenantId supplied in webhook payload without matching stored ProviderConfig.
