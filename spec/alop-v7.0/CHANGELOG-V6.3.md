# V6.3 Change Log — Payment Domain Hardening

## Major Changes
1. Payment model split into `PaymentOrder / PaymentAttempt / PaymentTransaction`.
2. Added multi-tenant `TenantPaymentMerchant` and SecretManager credential references.
3. Added explicit provider `SUCCESS / FAILED / UNKNOWN` contract.
4. UNKNOWN now blocks new payment attempts until provider query resolves it.
5. Added late-provider-success closed loop.
6. Added `PaymentChannelRequest` and payment/refund state logs.
7. Added Finance authoritative payment-target quote API.
8. Added Finance `RefundAmountReservation` to prevent concurrent over-refund.
9. Added payment/refund OpenAPI endpoints and provider callbacks.
10. Added PaymentSucceeded/Unknown/Closed/Refunded/RefundFailed event schemas.
11. Added payment-specific security spec, provider adapter spec, test suite and operations runbook.
12. Expanded payment error codes, permissions and dictionaries.
13. Fixed missing path parameter declarations in existing OpenAPI documents found during V6.3 validation.

## Compatibility Note
V6.3 supersedes the earlier simplified `PaymentOrder UNKNOWN/FAILED` modeling. Provider uncertainty is represented by `PaymentAttempt.UNKNOWN` and `RefundOrder.UNKNOWN`; PaymentOrder remains a logical payment intent with read-model `processingState=UNKNOWN` when applicable.
