# PAYMENT OPERATIONS RUNBOOK — V6.3

## 1. P0/P1 Conditions
### P0
- wrong tenant merchant used;
- same channelTradeNo mapped to multiple PaymentOrders;
- verified payment amount mismatch with local order;
- cross-tenant payment mutation;
- payment success causes duplicate Collection/Ledger effect.

### P1
- Payment UNKNOWN older than configured threshold;
- callback failure rate spike;
- refund UNKNOWN backlog;
- provider query unavailable;
- late-success rate abnormal.

## 2. Payment UNKNOWN
1. Open payment detail and inspect last ChannelRequest.
2. Trigger `Provider Query` only; do not create a new payment attempt.
3. If provider SUCCESS -> normal verified-success command.
4. If provider CLOSED/NOTPAY -> close/fail attempt.
5. If still UNKNOWN -> retry on schedule; after threshold create/assign IntegrationTask.
6. Never manually update status.

## 3. Client Says Paid, Local Not Paid
1. Search by paymentNo.
2. Query provider.
3. Search provider statement/channelTradeNo.
4. If provider confirms SUCCESS and identifiers/amount/merchant match, use verified query/reconciliation repair path.
5. Verify PaymentSucceeded Outbox and Finance Inbox.
6. Verify Collection exactly once.

## 4. Duplicate Callback
Expected behavior: callback log may contain duplicates; business transaction must remain one.
Check:
- provider trade unique key;
- PaymentOrder state log;
- Outbox event id/logical business event;
- Finance Inbox/source unique key.

Do not delete callback logs.

## 5. Late Success After Order Closed
1. Confirm provider success evidence and success time.
2. Use `recordLateSuccess` domain command, never generic status update.
3. Verify HIGH audit/metric.
4. Verify Finance Collection.
5. If source Reservation/Agreement no longer valid, move funds to unallocated/advance or refund through approved business flow.

## 6. Refund UNKNOWN
1. Do not release Finance refund reservation.
2. Query provider refund using refundNo/providerRequestNo.
3. SUCCESS -> persist RefundTransaction, emit PaymentRefunded, Finance confirm reservation.
4. Definitive FAILED -> emit failure/release reservation.
5. Repeated UNKNOWN -> IntegrationTask.

## 7. Provider/Merchant Mismatch
Treat as security/route incident.
- reject callback business mutation;
- preserve callback evidence hash/reference;
- inspect tenant_payment_merchant and TenantRoute;
- do not “fix” merchantId in payment row;
- escalate if another tenant merchant was involved.

## 8. Reconciliation Found Channel Success But Local Missing
Use reconciliation repair command only after exact match of:
- trusted merchant/app;
- paymentNo/business order;
- channelTradeNo;
- amount/currency;
- provider status.
Then create/confirm missing transaction through domain service and Outbox.

## 9. Forbidden Operations
- SQL `UPDATE payment_order SET status='SUCCESS'`;
- delete/rewrite PaymentTransaction;
- alter channelTradeNo;
- release refund reservation while provider result UNKNOWN;
- retry payment/refund provider create when result is UNKNOWN.
