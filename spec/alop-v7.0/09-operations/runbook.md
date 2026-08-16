# Operations Runbook Index
P0: cross-tenant leak/write, wrong payment merchant, wrong invoice tax identity, tenant route misrouting, ledger imbalance.
Operational consoles must support: Outbox retry, DLQ/IntegrationTask, Payment UNKNOWN query, Invoice UNKNOWN query, Sign Saga compensation, ES rebuild, reconciliation repair workflow, tenant provisioning retry.
No production direct UPDATE of payment/invoice/ledger facts.


## 水电/物业/车位 Runbook
- 抄表遗漏：补录 DRAFT -> 审核，不允许直接创建财务应收。
- 已出账读数错误：Correct Reading -> Adjustment Bill，不修改原账。
- 自动表离线：标记 METER_OFFLINE，进入人工复核/估算策略；估算必须显式 source/anomaly/audit。
- 车位重复占用：检查 ScheduleGuard、Reservation/Occupancy 与幂等记录，禁止手工删占用。
- 物业费争议：展示签约 chargeableAreaSnapshot、费率版本和 calculationTrace。


## Payment Runbook
详细支付异常处理见 `payment-runbook.md`，包含 UNKNOWN、重复回调、晚到成功、退款 UNKNOWN、商户错配与渠道成功本地缺失。
