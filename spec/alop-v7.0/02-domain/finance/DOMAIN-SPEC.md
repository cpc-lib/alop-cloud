# FINANCE DOMAIN SPEC

## 1. Bounded Context / Service
`alop-finance`

## 2. Aggregate Roots
- `Receivable`
- `Collection`
- `AccountingEntry`
- `ReconciliationBatch`
- `DunningCase`

## 3. Owned Tables
- `receivable`
- `receivable_adjustment`
- `collection_record`
- `payment_allocation`
- `allocation_reversal`
- `customer_advance`
- `invoice_quota_reservation`
- `account`
- `accounting_entry`
- `accounting_line`
- `dunning_case`
- `reconciliation_batch`
- `reconciliation_item`
- `reconciliation_exception`
- `channel_statement`
- `channel_statement_item`

## 4. Commands
- `CreateReceivableFromBill`
- `RecognizeCollection`
- `AllocateCollection`
- `ReverseAllocation`
- `WriteOffReceivable`
- `ReserveInvoiceQuota`
- `ConfirmInvoiceQuota`
- `ReleaseInvoiceQuota`
- `RunReconciliation`

## 5. Queries
- `Finance360`
- `ReceivableAging`
- `CollectionUnallocated`
- `InvoiceEligibility`

## 6. Produced Events
- `ReceivableCreated`
- `ReceivableOverdue`
- `CollectionCreated`
- `AllocationCreated`
- `AllocationReversed`
- `ReconciliationExceptionCreated`

## 7. Permissions
- `finance:receivable:view`
- `finance:manual-collection`
- `finance:allocation:reverse`
- `reconciliation:handle`

## 8. Invariants
- `receivableAmount=original+adjustment`
- `outstanding=receivable-allocated`
- `allocation <= collection.unallocated and receivable.outstanding`
- `ledger debit=credit`
- `0.01 RMB mismatch is mismatch`

## 9. Transaction / Locking
- `Collection FOR UPDATE; sorted Receivable FOR UPDATE; quota row FOR UPDATE`

## 10. Idempotency
- `Collection sourceType+sourceId unique`
- `AccountingEntry business posting key unique`

## 11. Closure Condition
Financial closed when outstanding is zero or approved write-off, advances have disposition, no in-flight refund/quota, ledger balanced and no unresolved CRITICAL reconciliation.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.


## 16. V6.3 补充费用财务要求
Receivable 必须保留具体 ChargeType：`WATER, ELECTRICITY, PROPERTY_MANAGEMENT_FEE, PARKING_RENT, PARKING_MANAGEMENT_FEE, EV_CHARGING_ELECTRICITY, UTILITY_ADJUSTMENT, PARKING_PENALTY`。核销、退款、开票和 Ledger 不得把这些费用合并成不可追溯的 `OTHER`。

已出账水电读数更正产生 Adjustment Bill -> 新 Receivable（正数补收或负数冲减流程），不得直接改原 Receivable 金额。

## 17. V6.3 Payment/Refund Integration Contract

### 17.1 Payment Target Quote
Payment Service 创建 PaymentOrder 前必须调用 Finance 获取权威支付目标：

`QuotePaymentTargets(customerId, targets[])`

Finance 返回每个目标：
- receivableId / businessId；
- receivableNo；
- chargeType；
- currentOutstandingAmount；
- allowedPayAmount；
- currency；
- customerId。

Payment Service 不得信任客户端金额。

### 17.2 RefundAmountReservation
Finance 新增 `RefundAmountReservation`，用于在 Payment 调渠道退款之前预占真实可退额度，防止并发超额退款。

状态：
- `RESERVED`
- `CONFIRMED`
- `RELEASED`
- `EXPIRED`

目标明细 `RefundReservationTarget` 支持：
- `ALLOCATION`
- `CUSTOMER_ADVANCE`

### 17.3 Reserve Refund
`ReserveRefundAmount(paymentOrderId, originalTransactionId, refundRequestId, requestedAmount, optionalAllocationIds)`：
1. 定位 PaymentSucceeded 对应 Collection；
2. 锁 Collection；
3. 计算可退：有效收款 - 已确认退款 - 已 RESERVED 未完成退款；
4. 根据指定/策略确定需反转的 Allocation/Advance；
5. 创建 Reservation + Targets；
6. 返回 `refundReservationId`。

### 17.4 Confirm Refund
消费 `payment.refund.succeeded.v1`：
- Inbox 幂等；
- RefundReservation FOR UPDATE；
- 若已 CONFIRMED 直接成功；
- 反核销/调整对应 Allocation 或 Advance；
- 更新 Collection 的净资金/退款摘要读字段（事实仍由原收款+退款事件构成）；
- 创建退款 Ledger；
- Reservation -> CONFIRMED；
- Outbox finance refund settled event（如消费者需要）。

### 17.5 Release Refund
Payment definitive FAILED/CANCELLED：
- Reservation -> RELEASED；
- 恢复可退款额度；
- 不创建资金分录。

### 17.6 UNKNOWN
Payment Refund UNKNOWN 时 Finance Reservation 必须保持 RESERVED。任何 TTL 自动释放策略都必须先确认 Provider 不可能成功；默认 UNKNOWN reservation 不自动过期释放。

## V6.4 Receivable Reminder Ownership
Finance Context owns debt due/overdue trigger facts because Receivable is the authoritative outstanding balance.

Jobs/events:
- `ReceivableDueReminderJob` -> due-soon triggers such as T-7/T-1;
- `ReceivableOverdueJob` -> `finance.receivable.overdue.v1` and Dunning progression;
- duplicate job executions use stable trigger keys and never directly send SMS/email.

Notification consumes these facts and applies tenant channel/template rules.
