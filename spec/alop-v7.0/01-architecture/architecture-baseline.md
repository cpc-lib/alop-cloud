# Architecture Baseline

## Context map
`Tenant/IAM/Organization` 是平台基础；`Asset` 提供资源经营事实；`CRM` 管理获客与商机；`Agreement` 管合同及履约；`Billing` 计算“该收多少”；`Payment` 管渠道交易；`Finance` 管“欠多少、收多少、核销到哪、如何记账、是否对平”；`Invoice` 管税务票据；`Search/Notification` 为事件驱动读模型/副作用。

## 依赖方向
- 业务同步 Command 只允许单向：Agreement -> Asset(CommitReservation)、Invoice -> Finance(ReserveQuota)、Payment -> Provider。
- 反向状态传播优先用事件，避免循环 Feign。
- 查询允许 Internal API，但禁止级联 5 层以上同步调用。

## 一致性分类
- Strong Local: Reservation+Items+ScheduleGuard；Finance 中 Receivable+Collection+Allocation+Ledger；PaymentOrder+Transaction+Outbox。
- Saga Business Consistency: Agreement Sign、Tenant Provisioning、Tenant Migration、Invoice Quota。
- Eventual: ES、Timeline、Dashboard、Notification、BI。

## V6.3 Supplementary Operations
Water/electric metering is an asset/operations fact; tariff and charge calculation belongs to billing. Property management fee is a versioned BillingRule with contract basis snapshot. Parking is a ResourceUnit and reuses the common reservation/occupancy inventory model. See ADR-011.

## V6.5 Service Boundary Extensions

New recommended modules:
- `alop-tax`: tax rules/calculation snapshots
- `alop-ap`: suppliers, payables, payment requests and payouts
- `alop-owner-settlement`: owner operating agreements and settlement batches

Existing boundaries extended:
- `alop-agreement`: AgreementParty, ResourceTransfer
- `alop-finance`: SecurityDepositAccount, UnidentifiedCollection
- `alop-billing`: UtilityUsagePeriod

Dependency direction:
OwnerSettlement -> AP
Billing -> Tax
Invoice -> Tax snapshot/business data
Finance -> Tax only when ledger posting requires tax split
Agreement ResourceTransfer -> Asset via Saga command; reverse direction uses events.
