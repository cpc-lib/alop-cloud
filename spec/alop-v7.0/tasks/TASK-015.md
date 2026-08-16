# TASK-015 — Payment Domain V7.0

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.


## 1. Business Goal
实现生产级多租户支付域，支持微信/支付宝/银联支付、支付尝试、可靠回调、UNKNOWN 查询恢复、订单关闭、部分/全额退款、退款财务额度预占、租户商户隔离和支付运营异常处理。

## 2. Bounded Context
`alop-payment`

## 3. Mandatory Input SPEC
- `00-master/MASTER-SPEC-V7.0.md`
- `02-domain/payment/DOMAIN-SPEC.md`
- `02-domain/payment/STATE-MACHINE.md`
- `01-architecture/adr/ADR-007-payment-tenant-merchant.md`
- `01-architecture/adr/ADR-012-payment-order-attempt-transaction.md`
- `01-architecture/adr/ADR-013-refund-finance-reservation.md`
- `03-database/flyway/payment/`
- `04-openapi/payment.yaml`
- `05-events/registry.md`
- payment event schemas
- `08-tests/payment.md`

## 4. Aggregate / Entity
### Aggregates
- `PaymentOrder`
- `RefundOrder`

### Entities
- `PaymentAttempt`
- `PaymentBusinessRelation`
- `PaymentTransaction`
- `RefundTransaction`
- `TenantPaymentMerchant`

### Value Objects
- `PaymentNo`
- `AttemptNo`
- `RefundNo`
- `Money`
- `ProviderRequestNo`
- `PaymentIntentHash`
- `MerchantIdentity`

## 5. Commands
Implement at minimum:
- `CreatePaymentOrderCommand`
- `CreatePaymentAttemptCommand`
- `HandlePaymentCallbackCommand`
- `QueryPaymentProviderCommand`
- `ClosePaymentOrderCommand`
- `RecordLatePaymentSuccessCommand`
- `CreateRefundCommand`
- `ApproveRefundCommand`
- `SubmitRefundToProviderCommand`
- `HandleRefundCallbackCommand`
- `QueryRefundProviderCommand`

## 6. Queries
- payment detail/status
- payment attempts
- transactions
- refunds
- UNKNOWN/exceptions
- tenant merchant metadata (never secrets)

## 7. Application Flow — Create Payment
1. Validate TenantContext and payment feature.
2. Validate customer belongs to current tenant.
3. Call Finance Internal API to calculate authoritative payable targets.
4. Compare optional client expected amount exactly.
5. Generate sorted canonical `intentHash`.
6. Find existing active order with same intentHash.
7. Persist PaymentOrder + BusinessRelations atomically.
8. Resolve tenant merchant.
9. Persist PaymentAttempt INITIATED.
10. Call provider outside long DB transaction.
11. Record PaymentChannelRequest.
12. Persist provider result:
   - SUCCESS -> PREPAY_CREATED + client action;
   - FAILED -> FAILED;
   - UNKNOWN -> UNKNOWN + operational event.
13. Return payment order/attempt.

## 8. Application Flow — Callback
1. Read exact raw body and headers.
2. Write callback ingress log/body hash.
3. Resolve merchant credentials without trusting tenantId.
4. Verify provider signature before business mutation.
5. Resolve global paymentNo and trusted Tenant.
6. Verify merchant/app/amount/currency/status.
7. Local transaction: PaymentOrder FOR UPDATE.
8. Upsert PaymentTransaction using provider unique key.
9. Idempotently mark Attempt/Order success.
10. State log + audit.
11. Save Outbox `payment.payment.succeeded.v1`.
12. Commit then provider-specific ACK.

## 9. UNKNOWN Flow
- Provider create/query uncertain -> Attempt UNKNOWN.
- No new Attempt while UNKNOWN.
- XXL Job queries provider.
- Query SUCCESS -> normal success flow.
- Query definitive fail/closed -> attempt final state.
- Repeated unknown beyond threshold -> `IntegrationTask WAITING_MANUAL`.
- No manual SUCCESS button.

## 10. Refund Flow
1. Validate successful payment.
2. Finance `ReserveRefundAmount`.
3. Create RefundOrder.
4. Start Flowable if policy requires.
5. Revalidate after approval.
6. Call Provider refund.
7. SUCCESS -> RefundTransaction + Outbox PaymentRefunded.
8. Finance confirms refund reservation and reverses financial effects.
9. FAILED/CANCELLED -> release refund reservation.
10. UNKNOWN -> keep reservation and query provider.

## 11. Database Deliverables
Must support V1 + `V2__payment_domain_hardening.sql`.
Generate MyBatis DO/Mapper/Repository for:
- payment_order
- payment_business_relation
- payment_attempt
- payment_transaction
- payment_callback_log
- payment_channel_request
- payment_order_state_log
- tenant_payment_merchant
- refund_order
- refund_transaction
- refund_order_state_log

## 12. Provider Adapters
Implement SPI and one mock provider contract test first.
Then adapters:
- WeChat
- Alipay
- UnionPay

Provider adapter may depend on SDK/HTTP; Domain cannot.
All adapters must return `SUCCESS | FAILED | UNKNOWN`, never boolean.

## 13. Tenant Merchant Rules
- Credential is SecretManager reference.
- No secret appears in API response/log/audit.
- Tenant A cannot use Tenant B merchant config.
- Callback tenant resolution uses trusted merchant identity + global paymentNo/provider trade no.

## 14. Idempotency
- Create Payment: Idempotency-Key + intentHash.
- Create Attempt: attempt request id + active attempt guard.
- Callback: provider trade unique key + state lock.
- Refund: Idempotency-Key + Finance refundReservationId.
- Provider request: providerRequestNo unique.

## 15. Events
Produce schemas exactly:
- `payment.payment.succeeded.v1`
- `payment.payment.unknown.v1`
- `payment.payment.closed.v1`
- `payment.refund.succeeded.v1`
- `payment.refund.failed.v1`

## 16. Permissions
- payment:create
- payment:view
- payment:query-provider
- payment:close
- payment:refund:apply
- payment:refund:approve
- payment:refund:execute
- payment:merchant:view
- payment:merchant:manage
- payment:exception:handle

## 17. Required Metrics
Implement all metrics listed in Payment Domain SPEC, including duplicate callback, amount mismatch, merchant mismatch, UNKNOWN age, late success and refund UNKNOWN.

## 18. Tests — Must Pass
### Domain
- state matrices for PaymentOrder/PaymentAttempt/RefundOrder.
- invalid transitions.

### Callback
- valid callback.
- duplicate callback x100.
- invalid signature.
- merchant mismatch.
- app mismatch.
- amount mismatch exactly 0.01.
- currency mismatch.
- same provider trade number mapped to another PaymentOrder -> conflict/P0.

### UNKNOWN
- create timeout -> UNKNOWN -> query SUCCESS.
- UNKNOWN blocks new attempt.
- repeated UNKNOWN creates IntegrationTask.

### Multi-channel
- failed WeChat attempt -> Alipay attempt allowed.
- UNKNOWN WeChat attempt -> Alipay attempt forbidden.

### Refund
- partial refund.
- multiple partial refunds.
- full refund.
- two concurrent refunds cannot exceed paid amount.
- provider UNKNOWN holds Finance reservation.
- provider FAIL releases Finance reservation.

### Tenant
- Tenant A cannot read Tenant B payment.
- Tenant A merchant cannot verify/process Tenant B payment.
- forged tenant header/callback tenant field does not change trusted resolution.

### Reliability
- RabbitMQ down: payment commits + Outbox pending.
- provider slow/timeout does not hold DB transaction open.
- Redis down does not affect correctness.

### Cross-context E2E
- PaymentSucceeded x100 duplicate deliveries -> Finance Collection exactly 1.
- reservation expired + late deposit payment -> orphan payment IntegrationTask/finance unallocated path.

## 19. Forbidden Implementation
- `markPaymentSuccess` public/admin API.
- trusting client SDK success.
- boolean provider result.
- calling Provider inside DB transaction while holding PaymentOrder lock.
- retrying UNKNOWN create without query.
- secrets in DB/log/API.
- direct modification of Bill/Receivable in payment-service.
- direct production SQL to repair PaymentTransaction.

## 20. Definition of Done
- Compile.
- Flyway applies cleanly.
- OpenAPI validates.
- Event schemas validate.
- Domain tests >= 90% coverage.
- Payment integration/application >= 80% targeted coverage.
- Testcontainers MySQL/RabbitMQ tests pass.
- duplicate callback 100x passes.
- UNKNOWN and refund reservation E2E pass.
- Tenant merchant isolation pass.
- no TODO/placeholder/manual-success path.
