# PAYMENT DOMAIN SPEC — V6.3

## 1. Bounded Context / Service
`alop-payment`

支付域负责“支付意图、渠道尝试、渠道交易事实、退款意图、渠道退款事实”。

支付域**不负责**应收核销、会计分录、发票或业务账单状态。支付成功必须通过事件交给 Finance Context 形成 Collection，再由 Finance 完成 Allocation 与 Ledger。

---

## 2. 领域边界

### 2.1 Owned Aggregates
- `PaymentOrder`：一笔逻辑支付意图，描述“客户需要为哪些业务支付多少钱”。
- `RefundOrder`：一笔逻辑退款意图。

### 2.2 Owned Entities
- `PaymentAttempt`：一次具体渠道拉起/预支付尝试。
- `PaymentBusinessRelation`：支付意图与 Receivable / Reservation Deposit 等业务目标的关系。
- `PaymentTransaction`：渠道确认的支付资金事实。
- `RefundTransaction`：渠道确认的退款资金事实。
- `PaymentChannelRequest`：对第三方渠道的 create/query/close/refund 请求审计事实。
- `PaymentCallbackLog`：支付/退款回调入口事实。
- `PaymentOrderStateLog` / `RefundOrderStateLog`：关键状态迁移记录。
- `TenantPaymentMerchant`：租户支付商户配置，只保存 SecretManager reference，不保存明文密钥。

### 2.3 External Dependencies
- Finance Internal API：查询可支付金额、业务支付目标、退款财务可退额度/退款预占。
- Tenant/IAM：TenantContext、租户状态和支付 Feature。
- SecretManager：商户私钥、API Key、证书引用。
- WeChat / Alipay / UnionPay Provider Adapter。
- RabbitMQ via Outbox。
- XXL-JOB：UNKNOWN 查询、订单关闭、退款 UNKNOWN 查询。

---

## 3. 为什么必须拆 PaymentOrder / PaymentAttempt / PaymentTransaction

### PaymentOrder
表示业务层的支付意图，例如：

```text
客户 C1001
为 Receivable R1 50,000 + R2 20,000
总计支付 70,000 CNY
```

### PaymentAttempt
表示用户某一次实际拉起渠道：

```text
Attempt 1: WECHAT_MINI_PROGRAM
Attempt 2: ALIPAY_APP
```

只有在前一个 Attempt 已明确 FAILED/CLOSED/EXPIRED 后，才允许安全创建新的 Attempt。若前一个为 UNKNOWN，禁止切换渠道，避免双重扣款。

### PaymentTransaction
表示渠道已经确认的资金事实，例如：

```text
providerTradeNo = 420000...
amount = 70,000.00
status = SUCCESS
```

因此：

```text
客户端支付 success != PaymentTransaction SUCCESS
PaymentAttempt SUCCESS != Finance Collection 已生成
PaymentOrder SUCCESS != Bill/Receivable 已结清
```

---

## 4. PaymentOrder Aggregate

### 4.1 Fields
- `paymentOrderId`
- `tenantId`
- `paymentNo`：全平台唯一。
- `customerId`
- `paymentScene`
- `currency`
- `amount`
- `subject`
- `description`
- `intentHash`
- `status`
- `activeAttemptId`
- `paidTransactionId`
- `expireAt`
- `paidAt`
- `closedAt`
- `lastStatusSource`
- `statusReasonCode`
- `version`

### 4.2 PaymentScene
- `RECEIVABLE_PAYMENT`
- `RESERVATION_DEPOSIT`
- `SECURITY_DEPOSIT`
- `UTILITY_PAYMENT`
- `PROPERTY_FEE_PAYMENT`
- `PARKING_RENT_PAYMENT`
- `MIXED_RECEIVABLE_PAYMENT`

PaymentScene 只用于支付展示/策略，不取代 `PaymentBusinessRelation`。

### 4.3 Behaviors
- `create()`
- `startAttempt()`
- `markPaying()`
- `succeed(transaction)`
- `close(reason)`
- `markPartialRefund()`
- `markRefunded()`
- `recordLateSuccess()`

禁止 `setStatus()`。

---

## 5. PaymentBusinessRelation

每一笔 PaymentOrder 至少一条 Relation。

字段：
- `businessType`
- `businessId`
- `businessNoSnapshot`
- `expectedAmount`
- `chargeTypeSnapshot`

BusinessType：
- `RECEIVABLE`
- `RESERVATION_DEPOSIT`
- `SECURITY_DEPOSIT`

创建支付订单时，**服务端必须重新从权威服务获取可支付金额**。客户端传入 `amount` 仅用于用户确认，不可作为最终金额依据。

---

## 6. Intent Hash / 防重复支付

为减少同一应收被重复拉起支付，PaymentOrder 生成 `intentHash`：

```text
SHA-256(
 tenantId |
 customerId |
 sorted(businessType:businessId:expectedAmount) |
 currency
)
```

创建 PaymentOrder 时：
- 若同 `intentHash` 已存在 `CREATED/PAYING` 且未过期订单（包括存在 UNKNOWN Attempt 的 PAYING Order），默认返回已有订单，或返回 `PAYMENT_ACTIVE_ORDER_EXISTS`。
- 若已有 `SUCCESS`，返回 `PAYMENT_BUSINESS_ALREADY_PAID_OR_PROCESSING`，并要求重新从 Finance 获取余额。
- 若已 CLOSED/FAILED 且业务仍有余额，可创建新 Order。

该机制是防误付优化；**最终财务超额保护仍由 Finance Allocation 实现**。

---

## 7. PaymentAttempt

### 7.1 Fields
- `attemptId`
- `attemptNo`
- `paymentOrderId`
- `channel`
- `paymentMethod`
- `merchantConfigId`
- `providerRequestNo`
- `providerPrepayId`
- `providerStatus`
- `status`
- `clientActionPayload`
- `expireAt`
- `unknownSince`
- `lastQueryAt`
- `queryCount`
- `version`

### 7.2 Channel
- `WECHAT`
- `ALIPAY`
- `UNIONPAY`
- `BANK_TRANSFER`（无第三方预支付时走人工/银行收款流程，不直接模拟成在线支付）

### 7.3 PaymentMethod / Scene
- `WECHAT_MINI_PROGRAM`
- `WECHAT_JSAPI`
- `WECHAT_NATIVE`
- `WECHAT_H5`
- `WECHAT_APP`
- `ALIPAY_APP`
- `ALIPAY_WAP`
- `ALIPAY_PC`
- `UNIONPAY_APP`
- `UNIONPAY_WEB`

具体 Provider Adapter 负责把内部 method 映射到渠道产品代码。

### 7.4 Attempt Rules
- 同一 PaymentOrder 同时最多一个 `PREPAY_CREATED/USER_PAYING/UNKNOWN` Attempt。
- 当前 Attempt = UNKNOWN 时禁止创建其他渠道 Attempt。
- FAILED/CLOSED/EXPIRED 后允许创建新 Attempt。
- PaymentOrder SUCCESS 后禁止再创建 Attempt。

---

## 8. PaymentAttempt State Machine

```text
INITIATED
  -> PREPAY_CREATED
  -> USER_PAYING
  -> SUCCESS

INITIATED/PREPAY_CREATED/USER_PAYING
  -> FAILED
  -> UNKNOWN
  -> CLOSED
  -> EXPIRED

UNKNOWN
  -> SUCCESS
  -> FAILED
  -> CLOSED
```

`UNKNOWN` 的含义是：请求已经可能到达渠道，但系统无法确定最终结果。UNKNOWN 不允许直接 retry create payment，必须先 query provider。

---

## 9. PaymentTransaction

只有渠道确认发生资金事实后创建/确认。

字段至少：
- `transactionId`
- `tenantId`
- `paymentOrderId`
- `attemptId`
- `channel`
- `providerMerchantId`
- `providerAppId`
- `channelTradeNo`
- `providerStatus`
- `transactionType = PAYMENT`
- `amount`
- `currency`
- `channelFeeAmount`
- `settlementAmount`
- `payerReferenceHash`
- `successAt`
- `notifyAt`
- `lastStatusSource`
- `rawPayloadHash`

唯一约束：

```text
(channel, providerMerchantId, channelTradeNo)
```

重复 callback/query 必须命中同一 Transaction。

---

## 10. Tenant Payment Merchant

支持：
- `PLATFORM_MERCHANT`
- `TENANT_MERCHANT`

字段：
- `tenantId`
- `channel`
- `merchantMode`
- `merchantId`
- `appId`
- `credentialRef`
- `certificateRef`
- `callbackProfile`
- `status`
- `priority`
- `effectiveFrom/effectiveTo`

### Rules
1. 密钥、私钥、API Key、平台证书私钥不得明文落库。
2. 数据库只保存 SecretManager reference。
3. 回调 Tenant Resolution 依赖 `channel + merchantId/appId + paymentNo/providerTradeNo`，**禁止相信 callback body 中 tenantId**。
4. Tenant A 不能使用 Tenant B merchant config。
5. PaymentOrder 必须保存实际使用的 merchant config snapshot/reference，便于历史追踪与密钥轮换。

---

## 11. Create Payment Closed Loop

```text
Client CreatePayment
-> validate Tenant ACTIVE/payment feature
-> validate Customer
-> call Finance GetPayableTargets
-> server recalculate amount
-> validate client expectedAmount if supplied
-> build intentHash
-> active-order dedup
-> BEGIN local TX
   -> create PaymentOrder
   -> create PaymentBusinessRelations
   -> audit
   -> outbox PaymentCreated(optional)
-> COMMIT
-> Resolve Merchant
-> Create PaymentAttempt
-> call Provider.create()
   -> SUCCESS: save prepay/client action, Attempt PREPAY_CREATED
   -> FAILED: Attempt FAILED
   -> UNKNOWN: Attempt UNKNOWN
-> return PaymentOrder + Attempt + ClientAction
```

注意：Provider create 调用**不得包在长时间数据库事务内**。

---

## 12. Provider Adapter Contract

```java
public interface PaymentProvider {
    ProviderCreatePaymentResult create(CreateProviderPaymentCommand command);
    ProviderPaymentQueryResult query(QueryProviderPaymentCommand command);
    ProviderCloseResult close(CloseProviderPaymentCommand command);
    ProviderRefundResult refund(CreateProviderRefundCommand command);
    ProviderRefundQueryResult queryRefund(QueryProviderRefundCommand command);
    VerifiedPaymentCallback verifyPaymentCallback(RawCallback callback);
    VerifiedRefundCallback verifyRefundCallback(RawCallback callback);
}
```

所有 ProviderResult 必须包含：
- `resultType: SUCCESS | FAILED | UNKNOWN`
- `providerCode`
- `providerMessage`
- `providerRequestId`
- `rawResponseHash`

严禁只返回 boolean。

---

## 13. Payment Callback Closed Loop

```text
HTTP Raw Body
-> ingress log/body hash
-> resolve merchant credential
-> verify signature with raw body
-> verify timestamp/nonce/request identity if provider supports
-> extract paymentNo/channelTradeNo
-> resolve Tenant using trusted merchant + globally unique paymentNo
-> load PaymentOrder tenant-scoped
-> verify merchant/app
-> verify currency
-> verify exact amount
-> verify provider payment state
-> BEGIN
   -> PaymentOrder FOR UPDATE
   -> upsert PaymentTransaction by provider unique key
   -> idempotent state transition
   -> PaymentOrder SUCCESS
   -> PaymentAttempt SUCCESS
   -> state log/audit
   -> Outbox PaymentSucceeded
-> COMMIT
-> return provider-specific ACK immediately
```

重复回调必须：
- 记录 callback log；
- 不重复 PaymentSucceeded 业务副作用；
- 返回渠道成功 ACK。

---

## 14. Client Result Is Not Truth

UniApp/React 收到微信/支付宝客户端 SDK `success` 只能：

```text
展示“支付结果确认中”
-> GET server payment status
```

禁止调用：

```text
POST /payment/mark-success
```

服务端最终结果只能来自：
- verified callback；
- provider query；
- reconciliation-confirmed repair command。

---

## 15. UNKNOWN Closed Loop

### 15.1 UNKNOWN Sources
- create request timeout after request sent；
- query timeout；
- callback processing前服务异常；
- provider 5xx but request may have succeeded；
- response parse failure after remote success is possible。

### 15.2 UNKNOWN Handling
```text
Attempt UNKNOWN
-> PaymentUnknownQueryJob
-> Provider.query(paymentNo/providerRequestNo)
-> SUCCESS: normal success transaction
-> FAILED/CLOSED: close/fail attempt
-> still unknown: exponential query schedule
-> threshold exceeded: IntegrationTask WAITING_MANUAL
```

UNKNOWN 时禁止：
- 创建新渠道 Attempt；
- 人工直接 mark SUCCESS；
- 自动退款；
- 把 Order 当 FAILED。

---

## 16. Late Success Policy

本地 Order 已 CLOSED，但渠道之后返回 SUCCESS：
- 不能丢弃真实资金事实；
- 记录 `LATE_PROVIDER_SUCCESS`；
- 创建/确认 PaymentTransaction；
- PaymentOrder 允许通过专用 `recordLateSuccess()` 进入 SUCCESS；
- 发布 PaymentSucceeded；
- 创建 HIGH 审计事件/指标；
- Finance 正常生成 Collection；
- 若原业务已经取消，Collection 会进入未分配/异常资金流程，由运营退款或重新分配。

禁止普通 API 直接执行 CLOSED -> SUCCESS。

---

## 17. Close Payment

支付关闭适用于：
- PaymentOrder 到期；
- Reservation 已取消；
- 用户主动放弃且 Provider 支持关闭；

流程：
```text
query provider first when status uncertain
-> provider close
-> SUCCESS: Attempt CLOSED, Order CLOSED
-> UNKNOWN: keep UNKNOWN and query
```

若渠道已 SUCCESS，close 必须失败并进入成功闭环。

---

## 18. Refund Domain

RefundOrder 表示一笔逻辑退款。

字段：
- `refundId/refundNo`
- `tenantId`
- `paymentOrderId`
- `originalTransactionId`
- `refundAmount`
- `reasonCode/reason`
- `refundReservationId`：Finance 退款额度预占 ID。
- `status`
- `providerRefundNo`
- `unknownSince`
- `approvedWorkflowInstanceId`
- `version`

### Refund Status
- `DRAFT`
- `PENDING_APPROVAL`
- `APPROVED`
- `PROCESSING`
- `UNKNOWN`
- `SUCCESS`
- `FAILED`
- `CANCELLED`

---

## 19. Refund Financial Reservation

为防止多个并发退款超过可退额度，退款必须先向 Finance 申请：

```text
ReserveRefundAmount(paymentOrderId, refundAmount, refundRequestId)
```

Finance 根据：
- Collection；
- Allocation；
- 已成功退款；
- 已预占未完成退款；

计算真实可退额度并返回 `refundReservationId`。

### Rules
- Refund SUCCESS -> Finance `ConfirmRefundReservation`，完成反核销/退款账务。
- Refund FAILED/CANCELLED -> `ReleaseRefundReservation`。
- Refund UNKNOWN -> 保持 RESERVED，禁止释放。
- 预占、确认、释放接口均必须幂等。

---

## 20. Refund Approval

租户按金额/场景配置：
- 无审批；
- 财务主管；
- 财务经理；
- 多级审批。

Flowable 只负责审批，`APPROVED` 后 Payment Domain 仍需重新校验：
- PaymentOrder 已成功；
- 退款额度预占仍有效；
- 当前累计退款未超过支付金额；
- merchant config 仍可用。

---

## 21. Refund Provider Closed Loop

```text
CreateRefund
-> Finance reserve refund amount
-> Approval if required
-> RefundOrder APPROVED
-> PROCESSING
-> Provider.refund
   SUCCESS -> RefundTransaction + RefundOrder SUCCESS + PaymentOrder refund summary + Outbox PaymentRefunded
   FAILED -> RefundOrder FAILED + release finance reservation
   UNKNOWN -> RefundOrder UNKNOWN + keep finance reservation
```

UNKNOWN：通过 `RefundUnknownQueryJob` 查询，不允许重新提交相同退款请求。

---

## 22. Partial / Full Refund

必须满足：

```text
SUM(success refunds + active refund reservations) <= original successful payment amount
```

PaymentOrder：
- successRefundedAmount = 0 -> `SUCCESS`
- 0 < successRefundedAmount < paidAmount -> `PARTIALLY_REFUNDED`
- successRefundedAmount = paidAmount -> `REFUNDED`

RefundOrder 本身保持独立历史，不合并覆盖。

---

## 23. Payment Channel Request Audit

每次外部 create/query/close/refund/queryRefund 必须记录：
- `requestNo`
- `tenantId`
- `channel`
- `merchantConfigId`
- `operationType`
- `businessId`
- `providerRequestId`
- `requestHash`
- `responseHash`
- `httpStatus`
- `resultType`
- `providerCode`
- `durationMs`
- `createdAt`

Payload 不得明文记录 Secret、完整银行卡、私钥、AccessToken。

---

## 24. Amount / Currency Rules

- Java：`Money(BigDecimal amount, Currency currency)`。
- MySQL：`DECIMAL(18,2)`。
- Provider API 最小货币单位：统一通过 adapter 转 `long minorUnits`。
- `amount > 0`。
- Payment Callback 必须 exact match，不允许 0.01 容差。
- Channel Fee 不得从客户应付金额中静默扣减；费用单独记录/结算。

---

## 25. Concurrency / Locks

### Payment Callback
```sql
SELECT * FROM payment_order
WHERE tenant_id=? AND payment_no=?
FOR UPDATE;
```

### Refund
```sql
SELECT * FROM payment_order ... FOR UPDATE;
SELECT refund orders / summarized successful amount under same lock strategy;
```

退款额度的最终并发控制还必须由 Finance `RefundReservation` 保证。

---

## 26. Idempotency Matrix

| Operation | Primary Key | Secondary Guard |
|---|---|---|
| Create PaymentOrder | tenant+api+Idempotency-Key | intentHash active order |
| Create PaymentAttempt | paymentOrder+attemptRequestId | one active/unknown attempt |
| Payment Callback | provider callback/request + body hash | channel+merchant+tradeNo unique |
| Provider Query Result | paymentOrder+provider status | transaction unique |
| Close | paymentOrder+closeRequestId | state guard |
| Create Refund | tenant+Idempotency-Key | refund request business key |
| Refund Provider | providerRequestNo | providerRefundNo unique |
| Refund Callback | provider callback id/body hash | providerRefundNo unique |

---

## 27. Commands
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

---

## 28. Queries
- `GetPaymentOrderQuery`
- `GetPaymentAttemptQuery`
- `ListPaymentTransactionsQuery`
- `GetRefundOrderQuery`
- `ListRefundsByPaymentQuery`
- `ListPaymentExceptionsQuery`

---

## 29. Produced Events
- `payment.payment.created.v1`
- `payment.payment.succeeded.v1`
- `payment.payment.unknown.v1`
- `payment.payment.closed.v1`
- `payment.refund.succeeded.v1`
- `payment.refund.failed.v1`

---

## 30. Consumed Events
可选使用：
- `asset.reservation.expired.v1`：若支付场景为 Reservation Deposit，尝试关闭未成功订单；若已 SUCCESS 则不得回滚资金，进入 orphan payment 处理。
- `agreement.agreement.closed.v1`：关闭不再允许继续支付的历史 PaymentOrder（按业务策略）。

---

## 31. Permissions
- `payment:create`
- `payment:view`
- `payment:query-provider`
- `payment:close`
- `payment:refund:apply`
- `payment:refund:approve`
- `payment:refund:execute`
- `payment:merchant:view`
- `payment:merchant:manage`
- `payment:exception:handle`

高风险：merchant manage、refund approve/execute、exception repair。

---

## 32. Audit
必须审计：
- PaymentOrder 创建/关闭；
- merchant config 变更；
- Provider 查询触发；
- Late Success；
- Refund 申请/审批/执行；
- UNKNOWN 人工介入；
- reconciliation repair 导致的支付状态确认。

支付 Callback 本身写 callback log；同一回调重复也可记录，但业务状态迁移只一次。

---

## 33. Metrics
至少：
- `payment_order_created_total`
- `payment_attempt_total{channel,result}`
- `payment_success_total{channel}`
- `payment_unknown_total{channel}`
- `payment_unknown_age_seconds`
- `payment_callback_total{channel,result}`
- `payment_callback_duplicate_total`
- `payment_amount_mismatch_total`
- `payment_merchant_mismatch_total`
- `payment_late_success_total`
- `refund_created_total`
- `refund_success_total`
- `refund_unknown_total`
- `provider_request_latency_seconds`

Prometheus 不建议 tenantId 作为无限高基数 Label。

---

## 34. Failure Classification

### Retryable
- DB transient/deadlock（幂等前提）；
- Provider query temporary failure；
- MQ publisher failure（Outbox retry）。

### Non-Retryable
- invalid signature；
- merchant mismatch；
- amount mismatch；
- currency mismatch；
- invalid domain state；
- refund exceeded。

### UNKNOWN
- request already sent but final provider outcome unknown。

---

## 35. Manual Repair Rules
人工页面允许：
- Trigger Provider Query；
- Trigger Reconciliation；
- Retry safe callback processing from saved verified payload/reference；
- Close known-unpaid order；
- Escalate。

禁止：
- 手工把 Payment 标成 SUCCESS；
- 手工改 paid amount；
- 删除 PaymentTransaction；
- 修改 channelTradeNo；
- UNKNOWN 未查渠道就重新退款。

---

## 36. Closure Conditions

### PaymentOrder financially observable closed
满足任一：
- SUCCESS/PARTIALLY_REFUNDED/REFUNDED 且所有 Provider facts 已落库；
- CLOSED 且不存在 UNKNOWN Attempt；
- UNKNOWN 必须被 query/reconciliation 最终解决。

### RefundOrder closed
- SUCCESS；
- FAILED + Finance refund reservation released；
- CANCELLED + reservation released。

UNKNOWN 不算闭环。

---

## 37. Mandatory Tests
1. Normal WeChat/Alipay/UnionPay payment.
2. Client reports success but server callback absent -> not success.
3. Duplicate callback 100 times -> one transaction/event/collection effect.
4. Wrong signature.
5. Wrong merchant/app.
6. Wrong amount 0.01.
7. Wrong currency.
8. Provider create UNKNOWN then query SUCCESS.
9. Provider query remains UNKNOWN -> IntegrationTask.
10. UNKNOWN blocks channel switching.
11. Failed attempt allows new attempt.
12. Late provider success after local close.
13. Two concurrent payment creates for same intent.
14. Tenant A merchant cannot process Tenant B payment.
15. Tenant header forgery callback ignored; trusted merchant/paymentNo resolves tenant.
16. Refund partial/full.
17. Concurrent refunds cannot exceed paid amount.
18. Refund UNKNOWN keeps Finance reservation.
19. Refund FAILED releases reservation.
20. PaymentSucceeded MQ duplicate -> Finance one Collection (cross-context E2E).
21. Reservation expired then late payment success -> orphan payment path.
22. RabbitMQ down -> payment commits, Outbox pending.
23. Redis down -> payment correctness unaffected.
24. Secret never appears in logs/audit payload.

---

## 38. Definition of Done
Payment module is not DONE until:
- schema/OpenAPI/event contracts validate;
- provider adapters pass contract tests;
- callback signature and tenant-resolution tests pass;
- UNKNOWN recovery and refund reservation flows pass;
- duplicate callback 100x test passes;
- Finance integration proves one PaymentSucceeded -> one Collection;
- no TODO/placeholder/manual-success API exists.
