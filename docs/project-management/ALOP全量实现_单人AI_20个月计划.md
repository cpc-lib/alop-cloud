# ALOP-SaaS 全量实现计划 — 单人 + AI

> 范围：后端 + React 管理端 + UniApp 用户端 + 测试 + 部署  
> 建议周期：**80 周 / 40 个双周 Sprint / 约 20 个月**  
> 建议商业承诺：**20～22 个月**  
> 假设投入：每周 45～50 小时，并使用 AI 加速样板代码、CRUD、DTO、前端表格/表单、测试骨架和文档。

## 为什么不是 8～12 个月

这是完整企业级多租户房源经营平台，不只是“房源 CRUD + 合同 + 收租”。V7.0 还包含：

- 多租户与 SupportSession
- 组织/管理团队/DataScope
- 资源排期与整租/分租冲突
- CRM/看房/报价/预订
- 合同多资源、多方主体、变更、续租、退租、换房 Saga
- 水电/物业/停车费
- Payment Attempt / UNKNOWN
- AR / Collection / Allocation
- 保证金负债子账
- 双分录 Ledger
- Reconciliation
- Invoice quota / 红冲 / 重开
- AP / Payout
- Owner Settlement
- Notification
- Operations WorkOrder
- ES Search
- RabbitMQ Outbox/Inbox
- Flowable
- XXL-JOB
- React 管理后台
- UniApp 用户端

AI 能提升编码速度，但不能替代资金正确性、并发、状态机、联调、测试和生产验证。

---

## 总览

| Phase | 周期 | Sprint | 后端 | React 管理端 | UniApp |
|---|---:|---:|---|---|---|
| P0 | W1-W6 | S1-S3 | Framework/Gateway/Tenant/IAM | 登录/权限/布局 | 登录/基础框架 |
| P1 | W7-W14 | S4-S7 | Organization/Asset | 组织/资产/团队 | 房源浏览基础 |
| P2 | W15-W22 | S8-S11 | Reservation/CRM | 排期/CRM | 看房/预约/报价 |
| P3 | W23-W32 | S12-S16 | Agreement/Workflow | 合同/审批/交接 | 合同/签署/续租 |
| P4 | W33-W42 | S17-S21 | Billing/Utility/Parking/Tax | 计费/水电/车位 | 账单明细 |
| P5 | W43-W52 | S22-S26 | Payment/Refund | 支付管理 | 收银台/支付 |
| P6 | W53-W64 | S27-S32 | Finance/Invoice/Reconciliation | 财务/发票/对账 | 发票/退款 |
| P7 | W65-W70 | S33-S35 | AP/OwnerSettlement | AP/业主结算 | N/A |
| P8 | W71-W74 | S36-S37 | Operations/Notification/Search | 工单/通知/搜索 | 工单/消息/搜索 |
| P9 | W75-W78 | S38-S39 | MQ/Saga/Sharding/Performance/Security | 全量回归 | 全量回归 |
| P10 | W79-W80 | S40 | RC / Production | RC | RC |

---

## P0 — Week 1-6 — Foundation

### Backend

- alop-dependencies
- alop-framework
- Gateway
- Tenant
- IAM
- Security
- TenantContext
- SupportSession
- RBAC
- DataScope
- MyBatis
- Redis/Redisson
- MinIO client
- audit
- idempotency base
- Docker Compose base

### React

- Vite + React + TS
- Ant Design
- Zustand
- Axios
- Tailwind
- Login
- Layout
- Dynamic Menu
- Route Permission
- API error handling
- Tenant switch context

### UniApp

- Login
- Token
- Request
- Store
- Base Navigation
- Profile skeleton

### Milestone M0

三端可登录；租户隔离生效；Platform SupportSession 流程骨架存在。

---

## P1 — Week 7-14 — Organization + Asset

### Backend

- OrganizationUnit
- ManagementTeam
- ResourceResponsibility
- ACL
- Asset
- Space
- ResourceUnit
- ConflictGroup
- Valuation
- Offering
- Listing
- ParkingSpaceProfile
- UtilityMeter

### React

- 组织树
- 团队
- 资源责任人
- 资产台账
- 房源树
- 房间/办公室/商铺/车位
- 评估
- Offering
- Listing

### UniApp

- 首页
- 房源列表
- 房源详情
- Listing 详情
- 车位基础展示

### M1

资产录入 → 审批入口 → 评估 → Offering → Listing 可跑通。

---

## P2 — Week 15-22 — Reservation + CRM

### Backend

- ResourceScheduleGuard
- Availability
- Reservation
- Occupancy precondition
- 整租/分租冲突
- Lead
- Customer
- Opportunity
- Viewing
- Quotation Version
- CRM Task/SLA
- Lost/Reopen

### React

- 房源排期日历
- Reservation
- Lead Pool
- Customer
- Opportunity
- Viewing
- Quotation
- CRM Task

### UniApp

- 预约看房
- 看房时间
- 报价
- Reservation
- 我的预约

### M2

Lead → Viewing → Quotation → Reservation 全链路完成；未来非重叠档期可提前出租。

---

## P3 — Week 23-32 — Agreement + Workflow

### Backend

- Flowable Adapter
- Agreement
- AgreementItem[]
- AgreementParty
- Snapshot
- Approval
- Signature
- SIGNED != EFFECTIVE
- Handover
- Occupancy
- RenewalPriority T-90
- AgreementChange
- Termination
- ResourceTransfer Saga
- CLOSED Gate

### React

- 合同模板
- 合同创建
- 合同审批
- 签署状态
- 交接
- 续租
- 退租
- 换房/换资源
- 合同变更

### UniApp

- 合同列表
- 合同详情
- 签署入口
- 续租
- 退租申请
- 交接确认

### M3

Reservation → Agreement → Sign → Effective → Handover → Occupancy 完成。

---

## P4 — Week 33-42 — Billing / Utility / Parking / Tax

### Backend

- BillingRule Version
- BillingPlan
- 1~12 月账期
- Bill / BillItem
- 租金
- Property Management Fee
- Water
- Electricity
- UtilityUsagePeriod
- MeterReading
- Parking Rent
- EV Charging
- Proration
- Discount / Free Period
- TaxCategory / TaxRule / TaxSnapshot
- MoveOut Final Billing

### React

- 计费规则
- Billing Plan
- 账单
- 水电表
- 抄表
- 物业费
- 车位费
- 税规则
- 计算轨迹

### UniApp

- 我的账单
- 账单详情
- 水电明细
- 物业费
- 车位费

### M4

合同 → BillingRule → Bill 形成完整账单闭环。

---

## P5 — Week 43-52 — Payment / Refund

### Backend

- PaymentOrder
- PaymentAttempt
- PaymentTransaction
- Merchant Config
- WeChat Pay
- Alipay
- UnionPay adapter
- Callback verify
- intentHash
- Duplicate callback
- Late Success
- UNKNOWN Query Recovery
- RefundAmountReservation
- RefundOrder / RefundTransaction

### React

- 支付订单
- 支付尝试
- 支付流水
- 退款
- UNKNOWN 异常中心
- 渠道配置

### UniApp

- 收银台
- 微信支付
- 支付宝
- 支付确认中
- 支付结果
- 退款结果

### M5

Bill 对应支付意图可完成安全支付，但 Payment Service 不直接修改 Receivable。

---

## P6 — Week 53-64 — Finance / Invoice / Reconciliation

### Backend

- Receivable
- Collection
- Allocation
- AllocationReversal
- CustomerAdvance
- SecurityDeposit
- UnidentifiedCollection
- Adjustment
- WriteOff
- Double-entry Accounting
- Daily Control
- Dunning
- PromiseToPay
- Reconciliation
- InvoiceQuotaReservation
- InvoiceApplication
- NuoNuo Adapter
- RedFlush
- Reissue
- Email delivery event

### React

- 应收
- 收款
- 核销
- 多收款
- 保证金
- 未识别收款
- 坏账
- Ledger
- 催收
- 对账
- 异常对账
- 发票
- 红冲
- 重开

### UniApp

- 应收余额
- 保证金
- 发票申请
- 发票详情
- 退款
- 催缴通知

### M6

Bill → Receivable → Payment → Collection → Allocation → Ledger → Invoice → Reconciliation 完整闭环。

---

## P7 — Week 65-70 — AP + Owner Settlement

### Backend

- Supplier
- SupplierInvoice
- Payable
- PaymentRequest
- Approval
- Payout
- Payout UNKNOWN
- Owner
- SettlementRule
- OwnerSettlementBatch
- OwnerStatement
- AdjustmentBatch
- Owner Payable → AP

### React

- 供应商
- 应付
- 付款申请
- Payout
- 业主
- 结算规则
- 业主结算批次
- 业主账单

### UniApp

无核心需求；只处理必要 owner/tenant portal 时再扩展。

### M7

AR 与 AP 完全分离；Owner Settlement 通过 AP Payout 出款。

---

## P8 — Week 71-74 — Operations + Notification + Search

### Backend

- OperationWorkOrder
- Maintenance
- Renovation
- Cleaning
- Inspection
- Security
- CustomerIssue
- SLA
- ConflictTask
- Notification Template
- SMS
- Email
- In-App
- Dedup
- Retry
- ES derived read model
- Reindex
- stale event protection

### React

- 工单
- 维修
- 装修
- SLA
- 通知模板
- 通知记录
- 搜索管理
- IntegrationTask

### UniApp

- 报修
- 工单进度
- 消息中心
- 搜索
- 合同/房源快速查询

### M8

运营售后和消息搜索完成。

---

## P9 — Week 75-78 — Production Hardening

### Backend

- Outbox / Inbox 全链路复核
- DLQ / Retry / Replay
- Persisted Saga recovery
- XXL-JOB recovery jobs
- ShardingSphere 仅热点事实
- Tenant isolation integration tests
- Payment/Refund/Payout/Invoice UNKNOWN chaos tests
- ResourceScheduleGuard concurrency tests
- Ledger balance tests
- T+1 reconciliation tests
- ES rebuild tests
- Security / IDOR / tenant bypass tests
- Performance tests

### React + UniApp

- 全量 E2E
- 权限回归
- 边界错误提示
- 移动端兼容
- 性能优化

---

## P10 — Week 79-80 — RC / V1.0

- 全量回归
- Flyway migration rehearsal
- Backup / restore
- Production config
- Secret management
- CI/CD
- Observability
- Runbook
- RC1
- RC2
- V1.0

---

# 单人 + AI 工作方式

建议每周 45~50 小时：

| 工作 | 比例 |
|---|---:|
| Java Backend | 45% |
| React | 20% |
| UniApp | 10% |
| Test / E2E | 15% |
| SPEC / Architecture | 5% |
| DevOps / Docs | 5% |

每个 Sprint 固定流程：

```text
Day 1      SPEC / State / DDL / OpenAPI
Day 2-5    Backend
Day 6-7    React
Day 8      UniApp
Day 9      Integration / E2E
Day 10     Bugfix / Docs / Release
```

AI 主力承担：

- DTO / VO
- Mapper / Converter
- CRUD
- OpenAPI boilerplate
- React Table / Form
- UniApp Skeleton
- Test Skeleton
- SQL migration draft
- 文档和 Mock

人工重点 Review：

- Tenant isolation
- Resource schedule conflict
- Agreement snapshots
- Billing
- Tax
- Payment
- Refund
- Security Deposit
- Collection / Allocation
- Accounting Entry
- Reconciliation
- Invoice quota
- AP / Payout
- Owner Settlement
- Saga / Idempotency

# 工期结论

全量实现建议：

> **80 周 / 20 个月 / 40 个双周 Sprint**

如果每周只能投入 25~30 小时：

> **约 26~30 个月**

如果主要目标是演示级 MVP，可以在 Week 32 左右形成：
资产 + CRM + Reservation + Agreement 主链路；
但本计划的目标是 **V7.0 全量企业级实现**，不是 MVP。
