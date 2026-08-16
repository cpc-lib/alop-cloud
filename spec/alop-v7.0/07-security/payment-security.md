# PAYMENT SECURITY SPEC — V6.3

## 1. Trust Boundaries
- Client/mobile/web result is untrusted.
- Callback body is untrusted until provider signature verification succeeds.
- callback/body/header `tenantId` is untrusted.
- Merchant credentials come only from server-side SecretManager references.
- Payment success evidence is trusted only after provider verification/query/reconciliation evidence.

## 2. Callback Processing Order
1. Preserve exact raw body bytes and relevant provider headers.
2. Resolve candidate merchant configuration from trusted routing metadata.
3. Verify signature/certificate on raw payload.
4. Only after verification parse business fields.
5. Resolve `paymentNo` and trusted tenant.
6. Verify merchant/app/order/amount/currency/status.
7. Execute idempotent state transition.

Do not deserialize/normalize/re-serialize and then verify a different payload.

## 3. Secret Storage
Never persist plaintext:
- WeChat API keys/private keys;
- Alipay application private keys;
- UnionPay signing private key/password;
- certificate private keys;
- provider access tokens.

Database stores `credentialRef/certificateRef` only.

## 4. Logs
Allowed:
- paymentNo/refundNo;
- masked merchantId;
- provider request id;
- channelTradeNo where business policy allows;
- payload hash;
- provider code;
- duration;
- traceId.

Forbidden:
- private key;
- API secret;
- full payer bank card;
- full identity number;
- access token;
- decrypted sensitive provider payload unless encrypted storage and explicit audit access are required.

## 5. Anti-Replay / Dedup
Use provider-supported request id/timestamp/nonce/certificate validation plus business unique constraints. Even when provider callback repeats legitimately, business processing is idempotent.

## 6. Merchant Isolation
`tenant_payment_merchant` is tenant-scoped. Provider callbacks are resolved by trusted merchant/app/payment identifiers. Cross-tenant merchant mismatch is P0.

## 7. Admin Security
High-risk actions require explicit permissions and audit:
- merchant config create/update/disable;
- refund approve/execute;
- provider query/reconciliation repair;
- late-success handling.

No admin endpoint may set PaymentOrder/RefundOrder to SUCCESS directly.

## 8. Internal API Security
Payment <-> Finance internal APIs require service identity/mTLS or equivalent service token, TenantContext propagation, request signature/trace and Idempotency-Key.
