# MASTER SPEC V7.0 — FROZEN CODEGEN BASELINE

## 1. 产品定位
ALOP-SaaS 是多租户企业级房源资产经营、招商 CRM、出租/销售、合同履约、计费、支付、收款核销、发票、账务、对账、审批、售后与运营一体化平台。支持住宅、长租公寓、写字楼、办公室、联合办公、商铺、酒店式公寓、会议室、工位、停车位、仓储等场景。

## 2. 六大业务闭环
1. **资产经营闭环**：资产录入 → 审批 → 评估 → Offering → Listing → Reservation/Agreement → 履约 → 退租/交割 → 再经营/归档。
2. **CRM 闭环**：Lead → Customer → Opportunity → Viewing → Quotation → Reservation → Agreement → Renewal/Lost。
3. **合同履约闭环**：DRAFT → 审批 → 签署 → SIGNED → EFFECTIVE → Handover → Change/Renew/Terminate → EXPIRED/TERMINATED → CLOSED。
4. **资金闭环**：BillingRule → Bill → Receivable → Payment → Collection → Allocation → Ledger。
5. **发票闭环**：Eligible Allocation → InvoiceQuotaReservation → Application → Invoice → RedFlush → Quota Restore → Reissue。
6. **对账闭环**：Provider Statement ↔ PaymentTransaction ↔ Collection ↔ Allocation/Receivable ↔ Invoice ↔ Ledger。

## 3. 多租户原则
- 所有业务事实必须属于唯一 Tenant；共享表全部 `tenant_id NOT NULL`。
- TenantContext 必须来自认证后的 membership，不信任客户端任意 `X-Tenant-Id`。
- 普通租户 API 不接受 body 中的 tenantId；由上下文注入。
- 缺失 TenantContext 时 **Fail Closed**，禁止退化成全表查询。
- Platform Admin 默认无权直接查看租户业务数据；必须使用有期限、有理由、可审计的 SupportSession。
- 支持三种隔离：Shared DB+Shared Schema、Shared DB+Separate Schema、Dedicated DB；V1 默认第一种。
- Redis/ES/MQ/MinIO/导出/缓存/权限全部带租户命名空间。

## 4. 核心领域模型
`Tenant, OrganizationUnit, ManagementTeam, UserMembership, Role, Permission, Asset, Space, ResourceUnit, ResourceConflictGroup, Valuation, Offering, Listing, Availability, Reservation, Occupancy, Lead, Customer, Opportunity, Viewing, Quotation, Agreement, AgreementChange, RenewalPriority, HandoverOrder, BillingRule, BillingPlan, Bill, Receivable, Collection, Allocation, PaymentOrder, PaymentTransaction, RefundOrder, InvoiceApplication, InvoiceQuotaReservation, Invoice, InvoiceRelation, AccountingEntry, ReconciliationBatch, DunningCase, OperationWorkOrder, UtilityMeter, MeterReading, UtilityTariffPlan, ParkingSpaceProfile, CustomerVehicle, ParkingVehicleBinding`。

## 5. 服务边界
- `alop-tenant`：Tenant 生命周期、套餐、Quota、Feature、Config、Route、迁移元数据。
- `alop-iam`：全局用户、租户 Membership、角色权限、认证、DataScope 定义。
- `alop-organization`：组织树、团队、资源责任关系、ACL、OwnershipHistory。
- `alop-asset`：Asset/Space/ResourceUnit/ConflictGroup/Valuation/Offering/Listing/Renovation/Maintenance，以及水电表计、抄表事实和 ParkingSpaceProfile（Reservation/Occupancy/Availability 已迁出，详见 ADR-023）。
- `alop-reservation`：Reservation/ReservationItem/ScheduleGuard/ResourceOccupancy/ResourceAvailability，本地维护 conflict_group 只读投影参与冲突预检。
- `alop-crm`：Lead/Customer/Opportunity/Viewing/Quotation/Activity/Task。
- `alop-workflow`：Flowable 适配、流程定义、业务流程关系、任务查询。
- `alop-agreement`：Agreement/Item/Snapshot/Change/RenewalPriority/Handover/Signature/Sign Saga。
- `alop-billing`：BillingRule/BillingPlan/Bill/BillItem，以及水电费率、物业管理费、停车费计算。
- `alop-tax`：TaxCategory/TaxRule、计税与税快照。
- `alop-payment`：PaymentOrder/PaymentTransaction/Refund/Provider Adapter/Callback。
- `alop-finance`：Receivable/Adjustment/Collection/Allocation/Reversal/Advance/Ledger/Reconciliation/Dunning/InvoiceQuota。
- `alop-invoice`：InvoiceApplication/Invoice/Relation/RedFlush/NuoNuo Adapter。
- `alop-ap`：Supplier/Payable/PaymentRequest/Payout。
- `alop-owner-settlement`：SettlementRule/OwnerSettlementBatch/OwnerStatement/OwnerPayable。
- `alop-notification`：模板、收件人、偏好、重试、去重。
- `alop-operations`：OperationWorkOrder/SLA/Handover 投影集成。
- `alop-search`：ES Search Read Model、重建、版本控制。
- `alop-file`：MinIO、文件元数据、病毒扫描、PreSigned URL、存储额度。
- `alop-infra`：Audit/Config/IntegrationTask/SecretRef/JobMeta 等平台基础设施。
- `alop-integration`：Provider/Webhook/CredentialMetadata 等外部集成元数据。

## 6. 关键不变量
### 6.1 房源库存
- `SOLD` 资源禁止新租赁；`VACANT_DELIVERY` 出售必须校验交割后无未来租约；`WITH_EXISTING_LEASE` 可保留既有租约但禁止新增出租。
- `RENOVATING/MAINTENANCE/FROZEN/ARCHIVED` 在冲突时间内不可新建租赁 Reservation。
- 时间区间统一 `[start,end)`；冲突公式 `existing.start < new.end && existing.end > new.start`。
- 当前在租不代表未来不可租；未来非重叠区间允许提前出租。
- 整租/分租使用 `ResourceConflictGroup`；整套与子房间在同一时间必须互斥。
- 所有资源排期变更先锁 `resource_schedule_guard`，按 resourceId 升序 `SELECT ... FOR UPDATE`。

### 6.2 合同
- 一份 Agreement 可含多个 AgreementItem；禁止 `contract.room_id` 单资源模型。
- 已签合同核心字段不可直接 UPDATE；使用 AgreementChange + 新 Snapshot。
- `SIGNED != EFFECTIVE`；未来合同可提前签署，到开始时间才 EFFECTIVE。
- `EXPIRED != CLOSED`；只有资源交接、Occupancy 结束、应收/保证金/退款/发票/对账全部闭环才 CLOSED。
- 续租默认创建新 Agreement，通过 `previous_agreement_id` 串联。

### 6.4 财务
- `Bill` 是客户账单，`Receivable` 是财务债权，二者不可合并。
- `Payment SUCCESS` 只表示渠道交易成功；Finance 消费事件创建 Collection，再通过 Allocation 核销 Receivable。
- Collection 多收形成 CustomerAdvance；少付保持 Receivable PARTIALLY_SETTLED。
- 财务历史禁止 DELETE；错误核销用 AllocationReversal；坏账用 WriteOffApplication/Adjustment；退款形成 Refund + Ledger。
- 保证金进入 `DEPOSIT_LIABILITY`，不得记租金收入；退租时通过 Receivable/Allocation/Refund 结算。
- Ledger 必须双分录且 `SUM(DEBIT)=SUM(CREDIT)`，否则事务回滚。

### 6.4 发票
- 开票依据是有效 Allocation，不是 PaymentOrder/Bill。
- 并发开票必须通过 `InvoiceQuotaReservation` 预占额度。
- Provider 超时使用 UNKNOWN 并查询，不得重复开票。
- 冲红必须创建独立 RedFlushApplication/红票/InvoiceRelation；红冲成功后恢复额度，重开必须新 Application。

### 6.5 对账
- T+1 渠道账与 PaymentTransaction 精确匹配，人民币不允许 0.01 容差。
- 对账层次：渠道↔支付、支付↔收款、收款↔核销/应收、核销↔发票、业务财务事实↔Ledger。
- 差异必须形成 ReconciliationException；不得用直接 SQL 改历史金额“修平”。

## 7. CRM 生命周期
Lead 进入池 → Assignment → SLA 首次联系 → QUALIFIED → Customer/Opportunity → Matching → Viewing → Quotation Versioning → Reservation → CONTRACTING → WON。任一阶段 LOST 必须记录 LostReason；可通过显式 `reopen` Command 重开。

## 8. 续租与退租闭环
- 默认 T-90 创建 RenewalPriority + Renewal Opportunity + CRM Task + Notification；租户可配置 30/60/90/120/180。
- STRICT：其他客户不可 Reservation；SOFT：可看房/报价但不可最终签约；NONE：无优先权。
- 提前退租：TerminationRequest → 费用/违约金计算 → 审批 → 终止协议 → MOVE_OUT → Damage/Outstanding 生成 Receivable → 保证金抵扣/退款 → Occupancy 结束 → TERMINATED → 满足财务/票据条件后 CLOSED。

## 9. 逾期催收闭环
Receivable 过期且 outstanding>0 → OVERDUE → DunningCase。支持 D+1 客户通知、D+3 客户经理、D+7 财务、D+15 主管、D+30 法务等租户策略。PromiseToPay 记录承诺金额/日期；逾期违约金必须生成新 Charge/Receivable，不直接改旧 Bill。

## 10. 装修/维修/工单闭环
Renovation/Maintenance 必须检查未来 Reservation/Occupancy；有冲突则创建 ConflictTask，不得强行覆盖。OperationWorkOrder 支持 MAINTENANCE/RENOVATION/CLEANING/INSPECTION/HANDOVER/SECURITY/CUSTOMER_ISSUE，并有 Tenant SLA。

## 11. 分布式一致性
- 服务内：MySQL 本地事务。
- 跨服务：Outbox + RabbitMQ + Inbox。
- Agreement Sign：持久化 Saga；Asset `CommitReservation` 成功而 Agreement 提交失败时创建可恢复 CompensationTask。
- Tenant Provisioning、Tenant DB Migration、Invoice Quota 等长流程必须持久化 Process/Saga 状态。
- Redis 仅用于加速锁、缓存、限流、幂等快速层，不作为业务最终事实。

## 12. 业务关闭定义
### Agreement CLOSED
必须同时满足：无未完成变更；MOVE_OUT/Handover 完成；Occupancy 结束；所有 Receivable SETTLED 或正式 WRITE_OFF；保证金已处理；无在途 Refund；无 UNKNOWN Invoice/RedFlush；无 CRITICAL Reconciliation；无 Sign/Finance Saga 异常。
### Resource Reusable
未 SOLD/FROZEN/RENOVATING/MAINTENANCE，且目标时间无 Reservation/Occupancy/ConflictGroup 冲突。
### Tenant TERMINATED
无在途支付/退款/开票/红冲/CRITICAL 对账；完成导出与 Retention；Route 终止。

## 13. 技术红线
Controller 不写领域规则；Domain 不依赖 MVC/MyBatis/Redis/RabbitMQ/Flowable；不跨服务写库；不 MySQL+ES 双写；不通用 UPDATE status；核心写 API 必须幂等；关键操作必须审计；Job Handler 只调 Application；Event 必须 versioned；Consumer 必须 Inbox 幂等。

## 14. Codegen Definition of Ready
每个 Task 开始前必须具备：Bounded Context、Aggregate、Invariants、State Machine、Tables/Indexes、Commands/Queries、OpenAPI、Events、Errors、Permissions、Transaction/Lock、Idempotency、Compensation、Tests。

## 15. Codegen Definition of Done
Compile + Unit + Domain + Integration + Tenant Isolation + Concurrency + Migration + OpenAPI + Event Schema + Permission + Audit + Metrics + README + SPEC Mapping 全部通过。


## 16. V6.4 水电费、物业管理费、车位租赁业务闭环

### 16.1 水费/电费闭环
`UtilityMeter -> MeterBinding -> MoveIn Baseline -> Period MeterReading -> Verify -> UtilityUsage Event -> UtilityTariff -> BillItem(WATER/ELECTRICITY) -> Receivable -> Payment -> Collection -> Allocation -> Invoice -> Ledger -> MoveOut Final Reading -> Final Utility Settlement`。

强规则：
- 水电费不能仅靠人工填写 Bill 金额；默认必须有有效读数/固定计费规则、费率版本和 calculationTrace。
- MeterReading 出账后不可覆盖；错误通过新版本读数 + Adjustment Bill/Receivable 纠正。
- 支持 DIRECT/SUBMETER/AREA_SHARE/FIXED_RATIO/MANUAL 共享表分摊；人工分摊必须权限、原因、附件与审计。
- MoveIn/MoveOut 读数必须纳入 Handover；存在应抄未抄或最终水电未结算时 Agreement 默认不能 CLOSED。
- 充电车位的电量计费复用 ELECTRICITY meter，但 ChargeType 为 `EV_CHARGING_ELECTRICITY`，保持业务用途可追溯。

### 16.2 物业管理费闭环
`Agreement/Offering -> PropertyManagementFee BillingRule -> BillingPlan -> BillItem(PROPERTY_MANAGEMENT_FEE) -> Receivable -> Collection/Allocation -> Invoice/Ledger`。

支持：
- `PER_AREA_PER_MONTH`：计费面积 × 单价 × 期间；
- `FIXED_PERIOD`；
- `PER_RESOURCE_PERIOD`；
- `PERCENTAGE_OF_RENT`。

签约时必须冻结 `chargeableAreaSnapshot`、费率、计算方式、周期和规则版本。后续房源面积或物业费率变化不得改变历史账单；通过 BillingRule 新版本从 effectiveFrom 起生效。

### 16.4 车位租赁闭环
车位不是附加文本字段，而是正式 `ResourceUnit(resourceType=PARKING_SPACE)`：
`Parking Resource -> Offering(PARKING_RENT) -> Listing -> Viewing/Quotation -> Reservation -> ScheduleGuard -> AgreementItem -> Occupancy -> Parking Rent Bill -> Receivable -> Payment/Collection/Allocation -> Invoice/Ledger -> Release`。

支持：
- 车位单独出租；
- 房屋/办公室 + 一个或多个车位同一 Agreement；
- 固定专属车位排他租赁，复用 ResourceScheduleGuard；
- 未来非重叠档期提前出租；
- CustomerVehicle 与 ParkingVehicleBinding，换车保留生效历史；
- STANDARD/MECHANICAL/EV_CHARGING/ACCESSIBLE 车位；
- 充电车位可绑定 Electricity Meter 单独计费。

### 16.4 新增核心 ChargeType
`PROPERTY_MANAGEMENT_FEE, WATER, ELECTRICITY, EV_CHARGING_ELECTRICITY, UTILITY_ADJUSTMENT, PARKING_RENT, PARKING_MANAGEMENT_FEE, PARKING_PENALTY`。

### 16.5 退租最终结算
MOVE_OUT 必须按合同规则完成：最后水/电读数 -> 未出账用量计费 -> 物业管理费截止日折算 -> 车位租金截止日折算 -> Damage/Outstanding -> 保证金抵扣/退款 -> Finance/Invoice/Reconciliation 完成，之后 Agreement 才能 CLOSED。

## 17. V6.4 支付域实施级闭环

### 17.1 支付对象三层模型
支付正式拆为：

```text
PaymentOrder      = 业务支付意图
PaymentAttempt    = 一次具体渠道拉起尝试
PaymentTransaction= 渠道确认的资金交易事实
```

一笔 PaymentOrder 可以经历多个明确失败/关闭后的 Attempt，但任何 `UNKNOWN` Attempt 未解决前禁止创建新 Attempt，以防双重扣款。

### 17.2 支付真相优先级

```text
Client SDK Result
< PaymentAttempt
< Verified PaymentTransaction
< Finance Collection
< Reconciliation
```

客户端微信/支付宝 SDK 返回 success 仅允许显示“确认中”，不得直接把服务器 Payment 标为 SUCCESS。

### 17.3 创建支付

```text
Client Targets
-> Finance authoritative payable quote
-> exact amount validation
-> Payment intentHash dedup
-> PaymentOrder + BusinessRelation
-> Tenant Merchant Resolution
-> PaymentAttempt
-> Provider create
-> SUCCESS / FAILED / UNKNOWN
```

Provider 网络调用不得长时间持有数据库事务锁。

### 17.4 UNKNOWN
任何请求已经可能到达渠道、但本地无法确认结果的情况都必须进入 UNKNOWN：

```text
UNKNOWN
-> XXL-JOB Provider Query
-> SUCCESS / definitive FAILED/CLOSED
-> repeated unknown -> IntegrationTask
```

UNKNOWN 禁止盲目重试 create/refund，禁止换支付渠道，禁止人工 mark SUCCESS。

### 17.5 支付回调

```text
Raw Body
-> resolve merchant credential
-> verify signature
-> resolve trusted tenant
-> verify merchant/app/paymentNo/amount/currency/provider status
-> PaymentOrder FOR UPDATE
-> unique PaymentTransaction
-> PaymentAttempt SUCCESS
-> PaymentOrder SUCCESS
-> Outbox PaymentSucceeded
-> ACK
```

重复回调 100 次必须只产生 1 个支付资金事实、1 次 PaymentOrder 成功迁移和 1 个逻辑 PaymentSucceeded 事件。

### 17.6 晚到成功
本地 PaymentOrder CLOSED 后如果渠道提供可信 SUCCESS 证据，不能丢弃真实资金。只能通过专用 `recordLateSuccess()`：
- 创建/确认 PaymentTransaction；
- PaymentOrder CLOSED -> SUCCESS；
- HIGH 审计和指标；
- Finance 创建 Collection；
- 若原 Reservation/Agreement 已取消，资金进入未分配/退款异常处理。

### 17.7 多租户商户
每次 Attempt 必须固化 `merchantConfigId`。密钥只存 SecretManager reference。回调 Tenant 由 `channel + merchant/app identity + global paymentNo/providerTradeNo` 决定，绝不信任 callback/body/header 中 tenantId。

### 17.8 退款财务预占
退款不是简单 `paid - refunded`。执行渠道退款前 Payment 必须调用 Finance：

```text
ReserveRefundAmount
-> refundReservationId
-> approval if needed
-> Provider Refund
```

结果：
- SUCCESS -> Finance Confirm Reservation + 反核销/退款 Ledger；
- FAILED/CANCELLED -> Release Reservation；
- UNKNOWN -> 保持 RESERVED 并查询渠道。

因此多个并发退款不能超过真实可退资金。

### 17.9 支付与应收
Payment Service 不直接修改 Bill/Receivable。唯一正式链路：

```text
PaymentSucceeded
-> Finance Collection
-> Allocation
-> Receivable
-> Ledger
```

超额到账进入 `CustomerAdvance`，不强行超额核销。

### 17.10 支付对账
渠道 T+1 对账必须能够证明：

```text
Provider Statement
= PaymentTransaction
= Collection
= Allocation/Advance
= Ledger
```

金额差 0.01 CNY 仍是异常。

### 17.11 支付禁止事项
- 禁止 public/admin `markPaymentSuccess` API；
- 禁止客户端 SDK success 作为资金事实；
- 禁止 Provider Adapter 只返回 boolean；
- 禁止 UNKNOWN 未查询就再次下单/退款；
- 禁止支付域直接修改 Receivable/Bill；
- 禁止明文商户 Secret；
- 禁止人工 SQL 修改 PaymentTransaction/channelTradeNo/status。

### 17.12 详细实现资料
以以下文档为代码生成权威输入：
- `02-domain/payment/DOMAIN-SPEC.md`
- `02-domain/payment/STATE-MACHINE.md`
- `03-database/flyway/payment/V2__payment_domain_hardening.sql`
- `03-database/flyway/finance/V2__refund_amount_reservation.sql`
- `04-openapi/payment.yaml`
- `04-openapi/finance.yaml`
- `05-events/schemas/payment-*.json`
- `08-tests/payment.md`
- `09-operations/payment-runbook.md`
- `tasks/TASK-015.md`


# V6.4 — SMS / EMAIL REMINDER + INVOICE EMAIL DELIVERY

## Notification Service mandatory scope
`alop-notification` becomes a formal Bounded Context. Business services do not call SMS/Email SDKs directly.

Channels required in V6.4:
- `IN_APP`
- `SMS`
- `EMAIL`

Business service responsibility: determine business trigger and publish event.
Notification responsibility: resolve recipient, template, channel, quiet hours, preference, dedup, provider, retry, fallback, receipt and audit.

## Reminder closed loop
At minimum support:
- Agreement expiry T-90/T-60/T-30/T-15/T-7/T-1;
- renewal priority start/expire;
- Bill issued and due reminders;
- overdue/Dunning reminders;
- Reservation expiry warning;
- Payment success/refund result;
- Viewing/CRM follow-up tasks;
- WorkOrder SLA violations;
- Invoice issued email delivery.

Tenant can configure channel policy such as `SMS+EMAIL`, trigger days, quiet hours and fallback. Every reminder must have a stable triggerKey/dedup key so duplicate XXL-JOB/MQ events cannot create duplicate customer messages.

## SMS rules
SMS is a provider strategy with tenant-level or platform-level provider config. Credentials live in SecretManager. Transactional/legal SMS is isolated from marketing consent and rate limits. Provider receipt is idempotent.

## Email rules
Email supports HTML + plain-text fallback, TO/CC/BCC, tenant branding, attachments, Reply-To and provider delivery/bounce callbacks where supported. Never claim DELIVERED when provider can only confirm SMTP acceptance.

## Invoice email delivery
After Invoice is legally `ISSUED` and electronic file metadata is ready:
`Invoice -> InvoiceDeliveryInstruction -> Outbox invoice.invoice.delivery-requested.v1 -> Notification -> EmailProvider -> DeliveryResult`.

Rules:
- email failure never changes Invoice `ISSUED` status;
- auto-send is deduplicated;
- manual resend creates a new immutable delivery instruction/history;
- recipient email is encrypted and not placed in MQ payload;
- PDF/OFD bytes are never placed in RabbitMQ;
- Notification fetches attachment securely from File Service by fileId;
- bounce/failure creates visible delivery history and, after retry exhaustion, IntegrationTask;
- admin invoice page displays masked recipient, send status, sent time, retries and failure/bounce reason.

## Notification architecture red lines
- no business service directly calls SMS/SMTP provider;
- no raw phone/email in MQ events;
- no duplicate send on duplicate business event;
- no silent terminal failure for transactional/legal notification;
- no resend while provider result is uncertain unless provider-specific policy proves it safe;
- no invoice tax status mutation to repair email delivery failure.

## New Codegen Task
`TASK-025 — Notification Center + Invoice Email Delivery` is required before production release.

# V6.5 — ENTERPRISE OPERATIONS HARDENING

V6.5 freezes eight additional enterprise capabilities before the next architecture baseline.

## 18. Agreement Party
Agreement legal/commercial participants are explicit and effective-dated:
LESSOR / LESSEE / PAYER / USER / GUARANTOR / OWNER / OPERATOR / INVOICE_PARTY / BROKER.

Rules:
- signed party snapshots are immutable;
- changing a signed party requires AgreementChange(PARTY_CHANGE);
- payer and invoice party may differ from lessee;
- cross-tenant party links are forbidden.

Authority:
`02-domain/agreement/AGREEMENT-PARTY-SPEC.md`

## 19. Security Deposit
Deposits become a dedicated Finance liability subledger:
SecurityDepositAccount + append-only SecurityDepositTransaction.

Rules:
- receipt is DEPOSIT_LIABILITY, not rent income;
- move-out deduction settles real Receivables;
- refund amount is reserved before provider execution;
- provider UNKNOWN retains refund reservation;
- Agreement cannot become CLOSED while deposit remains unsettled.

Authority:
`02-domain/finance/SECURITY-DEPOSIT-SPEC.md`

## 20. Utility Usage Period
Water/electricity calculation consumes immutable UtilityUsagePeriod snapshots.

Must support:
meter replacement, resets, multiplier changes, estimated readings, corrections,
shared master meters, submeter/allocation, time-of-use and tiered tariff.

A BILLED usage period is never edited. Corrections generate a new usage-period version
and adjustment Bill/Receivable.

Authority:
`02-domain/utility-property-parking/UTILITY-USAGE-PERIOD-SPEC.md`

## 21. Unidentified Collection
Real bank money that cannot be matched safely is never silently discarded or forced onto a customer.

Flow:
Statement -> UnidentifiedCollection -> candidate matching -> finance review/approval ->
Collection -> Allocation/CustomerAdvance -> Ledger -> Closed.

Amount/currency/source are immutable; correction uses reversal + re-claim.

Authority:
`02-domain/finance/UNIDENTIFIED-COLLECTION-SPEC.md`

## 22. Resource Transfer
Changing room/office/parking resource is a first-class persisted Saga.

Flow:
target eligibility -> target Reservation -> approval -> supplementary signature ->
target commit -> source move-out -> target move-in -> Occupancy switch ->
AgreementChange -> BillingRule version -> adjustment Bill/Receivable.

Do not model a change-room operation as arbitrary REMOVE_RESOURCE + ADD_RESOURCE.

Authority:
`02-domain/agreement/RESOURCE-TRANSFER-SPEC.md`

## 23. Tax Domain
Introduce effective-dated TaxCategory/TaxRule.

Billing/Invoice must snapshot:
tax category, tax mode, rate, net amount, tax amount, gross amount.

Old Bills/Invoices never recalculate using a newly changed rate.

Service/module recommendation:
`alop-tax`

Authority:
`02-domain/tax/DOMAIN-SPEC.md`

## 24. Accounts Payable
Introduce AP for money owed by the Tenant:
Supplier -> SupplierInvoice/source -> Payable -> PaymentRequest -> Approval ->
Payout -> Ledger.

AR and AP are intentionally separate.
External payout UNKNOWN follows the same safety principle as payment/refund:
query provider; never blindly repeat money movement.

Service/module recommendation:
`alop-ap`

Authority:
`02-domain/ap/DOMAIN-SPEC.md`

## 25. Owner Settlement
For managed-property/agency operating models:

eligible allocated revenue
-> SettlementRule
-> OwnerSettlementBatch
-> OwnerStatement
-> OwnerPayable(AP)
-> Payout
-> Ledger/Reconciliation.

Closed settlement batches are immutable; corrections use adjustment batches.

Service/module recommendation:
`alop-owner-settlement`

Authority:
`02-domain/owner-settlement/DOMAIN-SPEC.md`

## 26. V6.5 Codegen Tasks
- TASK-026 Agreement Party
- TASK-027 Security Deposit
- TASK-028 Utility Usage Period
- TASK-029 Unidentified Collection
- TASK-030 Resource Transfer
- TASK-031 Tax Domain
- TASK-032 Accounts Payable
- TASK-033 Owner Settlement

## 27. Additional Architecture Red Lines
- do not represent all agreement parties with one customer_id;
- do not treat deposit receipt as income;
- do not modify billed utility usage in place;
- do not discard unmatched real bank money;
- do not execute change-room as direct resource replacement;
- do not recalculate historical tax using current rules;
- do not reuse customer Receivable as supplier Payable;
- do not edit a CLOSED owner settlement batch.

## 28. V7.0 Freeze Criteria
Before declaring the next frozen baseline:
1. all V6.5 migrations validate;
2. all OpenAPI and Event schemas parse;
3. tenant-isolation tests cover all eight extensions;
4. deposit/AP/owner-settlement accounting scenarios balance;
5. resource-transfer Saga has rollback/compensation coverage;
6. historical tax and utility snapshots are immutable;
7. no unresolved SPEC-GAP remains.


# V7.0 — FROZEN CODEGEN BASELINE

## 1. Freeze Declaration
V7.0 freezes the bounded contexts and financial/resource invariants defined by V6.5. From this version onward, normal implementation work MUST NOT add a new bounded context, merge existing financial facts, or change resource inventory correctness rules without a new ADR and explicit architecture review.

### Frozen bounded contexts
- Tenant / SaaS
- IAM & Organization
- Asset / Resource Inventory
- Reservation
- CRM / Quotation
- Agreement / AgreementParty / ResourceTransfer / Handover
- Billing / Utility / Property Management Fee / Parking
- Tax
- Payment
- Finance AR / SecurityDeposit / UnidentifiedCollection / Ledger / Reconciliation
- Invoice
- Accounts Payable
- Owner Settlement
- Notification
- Operations
- Platform Integration / Audit / File / Search

## 2. Canonical Truth Boundaries
- Tenant routing truth: Tenant Service metadata store.
- Resource inventory truth: MySQL `ResourceScheduleGuard` + Availability/Reservation/Occupancy; Redis is only an optimization.
- Agreement legal truth: signed immutable snapshots + effective-dated changes.
- Billing truth: BillingRule/BillingPlan/Bill and immutable calculation trace.
- Payment channel truth: verified PaymentTransaction; client SDK success is never authoritative.
- Enterprise cash truth: Finance Collection.
- AR settlement truth: Receivable + Allocation.
- Deposit truth: SecurityDepositAccount + append-only transactions.
- AP truth: Payable + approved Payout.
- Tax truth: effective-dated TaxRule + bill/invoice snapshot.
- Invoice truth: provider-confirmed Invoice plus red-flush/reissue chain.
- Accounting truth: balanced AccountingEntry/AccountingLine.
- External consistency proof: Reconciliation.

## 3. Code Generation Contract
Every implementation task MUST read:
1. MASTER-SPEC-V7.0.md
2. relevant Domain SPEC(s)
3. DDL migrations + DATA-DICTIONARY.md
4. relevant OpenAPI file(s)
5. Event JSON Schema + event-registry.yaml
6. state-machines.yaml
7. transaction-lock-matrix.yaml
8. idempotency-matrix.yaml
9. permissions/error registries
10. task file + acceptance traceability rows

AI Coding MUST emit a SPEC implementation mapping and MUST NOT invent a different aggregate/service boundary when the baseline already defines one.

## 4. Frozen Architectural Red Lines
- No cross-tenant read/write without explicit platform SupportSession policy.
- No generic status setter endpoints.
- No Redis-only inventory correctness.
- No direct mutation/deletion of historical financial facts.
- No Payment -> Bill paid direct update.
- No direct MySQL + Elasticsearch dual write.
- No Flowable runtime table as business truth.
- No blind retry for payment/refund/AP payout/invoice result UNKNOWN.
- No direct edit of signed AgreementParty, tax snapshot, billed UtilityUsagePeriod or closed settlement batch.
- No owner settlement payout outside AP.
- No AP/AR merge into one ambiguous balance table.

## 5. Canonical Technical Types
- IDs: Java `Long`, MySQL `BIGINT`, generated by Snowflake; external event IDs UUIDv7/ULID.
- Money: `BigDecimal` / `DECIMAL(18,2)` + ISO-4217 currency; no float/double for money.
- Utility quantities: `BigDecimal` / `DECIMAL(20,6)`.
- Rates/multipliers: `BigDecimal` / `DECIMAL(20,8)` where defined.
- Business timestamps: UTC-normalized persistence plus asset/tenant IANA timezone context; API ISO-8601.
- Business interval semantics: `[start,end)`.
- Optimistic edit version: integer `version`.

## 6. Release Freeze Gates
V7.0 is implementation-ready only when all automated checks in `13-acceptance/RELEASE-GATES.md` pass. Breaking schema/event/API changes after V7.0 require a versioned contract and ADR.
