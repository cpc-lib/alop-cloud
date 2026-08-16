# ADR-013 Refund Finance Reservation

## Status
Accepted in V6.3.

## Context
同一支付可以部分退款多次；同时可能存在已核销应收、保证金、预收款和多个并发退款申请。只在 payment-service 计算 `paid - refunded` 无法证明财务上当前真正可退，也无法防止并发退款和财务反核销竞争。

## Decision
退款执行前必须调用 Finance 创建 `RefundAmountReservation`（实现表可位于 finance-service）。Payment Domain 保存 `refundReservationId`。

状态规则：
- Refund SUCCESS -> Confirm Reservation；
- Refund FAILED/CANCELLED -> Release Reservation；
- Refund UNKNOWN -> Reservation 保持占用；
- Reserve/Confirm/Release 均幂等。

## Consequences
退款不会超过真正可退资金，并能和 Allocation/Ledger 形成一致闭环。代价是退款成为跨 Payment/Finance 的可恢复 Saga。
