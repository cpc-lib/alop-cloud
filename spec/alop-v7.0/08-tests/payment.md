# PAYMENT TEST SPEC — V6.3

## 1. Test Objective
证明支付域在多租户、重复回调、超时 UNKNOWN、并发退款、跨服务重复消息和第三方故障下仍不会：
- 重复确认支付；
- 重复生成收款；
- 超额退款；
- 串租户商户；
- 因客户端结果错误修改资金事实。

## 2. Core Matrix

| ID | Scenario | Expected |
|---|---|---|
| PAY-001 | 正常微信小程序支付 | Order SUCCESS, Attempt SUCCESS, Transaction 1, Event 1 |
| PAY-002 | 客户端 SDK success 无服务端证据 | Order not SUCCESS |
| PAY-003 | 同回调100次 | Transaction=1, success transition=1, Outbox logical event=1 |
| PAY-004 | 签名错误 | no business mutation |
| PAY-005 | merchant错误 | no mutation + security metric |
| PAY-006 | 金额100.00 vs 99.99 | PAYMENT_AMOUNT_MISMATCH |
| PAY-007 | 币种错误 | PAYMENT_CURRENCY_MISMATCH |
| PAY-008 | create provider timeout | Attempt UNKNOWN |
| PAY-009 | UNKNOWN query success | Transaction created + Order SUCCESS |
| PAY-010 | UNKNOWN时切支付宝 | rejected |
| PAY-011 | WeChat FAILED后切Alipay | allowed |
| PAY-012 | CLOSED后晚到渠道SUCCESS | late-success path, Collection still generated |
| PAY-013 | Tenant A伪造Tenant B merchant | rejected |
| PAY-014 | 相同intent并发创建20次 | one active logical order or deterministic conflict |
| PAY-015 | RabbitMQ down | local success + Outbox pending |

## 3. Refund Matrix
| ID | Scenario | Expected |
|---|---|---|
| REF-001 | 100支付退30 | PARTIALLY_REFUNDED |
| REF-002 | 再退70 | REFUNDED |
| REF-003 | 并发退80+80 | successful/reserved total <=100 |
| REF-004 | Provider退款UNKNOWN | reservation remains RESERVED |
| REF-005 | UNKNOWN query SUCCESS | confirm Finance reservation once |
| REF-006 | Provider definitive FAIL | release Finance reservation |
| REF-007 | 重复退款callback100次 | refund fact/event once |
| REF-008 | 已全退再次退款 | REFUND_AMOUNT_EXCEEDED/invalid state |

## 4. Tenant Isolation
- Same paymentNo is globally unique, but repository queries still require tenant scope.
- Provider callback tenant is resolved from trusted merchant identity + paymentNo.
- Callback body `tenantId` is ignored even if present.
- Tenant A merchant credential reference must never be loaded for Tenant B order.

## 5. Provider Contract Test
Every adapter must pass a shared contract test suite:
- create SUCCESS / FAILED / UNKNOWN;
- query SUCCESS / FAILED / UNKNOWN;
- close;
- refund SUCCESS / FAILED / UNKNOWN;
- queryRefund;
- invalid callback signature;
- valid callback canonicalization;
- raw body preserved.

## 6. Transaction Assertions
- External provider network calls do not occur while holding long-lived DB locks.
- Callback state transition uses `PaymentOrder FOR UPDATE`.
- `payment_transaction` provider unique constraint prevents duplicate facts.
- refund financial reservation is confirmed/released idempotently.

## 7. Security Assertions
- no private key, API key, certificate secret, payer full sensitive data in logs.
- callback raw payload if retained is encrypted/object-stored with access control; log only hash/reference.
- merchant config API only returns masked metadata.
