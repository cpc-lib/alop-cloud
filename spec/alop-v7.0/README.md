# ALOP-SaaS V7.0 Codegen-Ready SPEC Repository

本仓库是“多租户企业级房源运营管理平台”的代码生成基线。目标是让 Spring Cloud 团队或 AI Coding Agent 在不重新解释核心业务模型的情况下，按 TASK 逐模块生成生产级代码。

## 使用顺序
1. 先读 `00-master/MASTER-SPEC-V7.0.md`。
2. 再读当前模块 `02-domain/<domain>/DOMAIN-SPEC.md`、`STATE-MACHINE.md`。
3. 同时加载 `03-database/flyway/<service>/`、`04-openapi/<service>.yaml`、`05-events/registry.md` 与相关 JSON Schema。
4. 加载 `10-registries/error-codes.yaml`、`permissions.yaml`、`dictionaries.yaml`。
5. 最后读取 `tasks/TASK-xxx.md` 并生成代码。

## 关键架构红线
- 多租户 Fail Closed：没有合法 TenantContext 时，业务查询/写入必须失败。
- 资源未来可用性由 `Availability + Reservation + Occupancy + ResourceConflictGroup + ResourceScheduleGuard` 决定，Redis 不是最终正确性来源。
- `Agreement EXPIRED != CLOSED`；只有资源、应收、保证金、退款、发票、对账全部闭环才能 CLOSED。
- `Payment SUCCESS != Bill paid`；资金链必须经过 `Payment -> Collection -> Allocation -> Receivable -> Ledger`。
- 财务事实禁止物理删除；使用调整、反核销、冲正、退款、坏账核销。
- 跨服务采用 Local TX + Outbox + RabbitMQ + Inbox；关键跨域流程使用可恢复 Saga。
- Flowable 只编排人工审批，不作为业务状态事实源。
- MySQL 是业务事实源；ES 是搜索读模型。

## 推荐代码生成顺序
`TASK-001 -> TASK-025`，每个 TASK 完成后必须编译、运行测试、生成 Migration/OpenAPI/Event Schema，并做 SPEC 映射自检。


## V6.2 已纳入经营能力：水电费 / 物业管理费 / 车位租赁
- 水费、电费不再只是账单枚举：加入 `UtilityMeter / MeterBinding / MeterReading / UtilityTariffPlan`，支持人工抄表、自动表计、导入、共享表分摊、异常复核、换表、入住底数、退租终读数和已出账后的更正账单。
- 物业管理费正式进入 Billing Engine：支持按计费面积×单价、固定额、按资源、按租金比例，支持 1~12 月周期、免收/折扣、费率版本与合同快照。
- 车位作为正式 `ResourceUnit(PARKING_SPACE)`：支持独立出租、与住宅/办公室同一 Agreement 多资源出租、ScheduleGuard 防重复出租、停车位属性、车辆档案/车牌绑定、充电车位电量计费。
- 新增 `TASK-024`，用于一次性生成上述领域的 Java/DDL/OpenAPI/Event/Test 实现。


## V6.4 支付域生产级细化
- 正式拆分 `PaymentOrder / PaymentAttempt / PaymentTransaction`，避免业务支付意图、渠道尝试和资金事实混用一个状态。
- 新增 Tenant Payment Merchant、SecretManager reference、商户/应用/回调 Tenant Resolution 规则。
- 新增支付 `UNKNOWN` 查询恢复、禁止盲目重试/换渠道、晚到成功处理和支付异常运营台。
- 退款升级为 Payment + Finance 可恢复闭环：先 `ReserveRefundAmount`，成功 Confirm，失败 Release，UNKNOWN 保持 RESERVED。
- 支付 OpenAPI、Flyway V2、PaymentSucceeded/Unknown/Closed/Refunded JSON Schema、专项安全规范、Provider Adapter SPEC、测试矩阵和 Runbook 已同步。
- `TASK-015` 已改成可直接驱动 Spring Cloud / AI Coding 的支付代码生成任务。

## V6.4 重点入口
- `CHANGELOG-V6.4.md`
- `02-domain/payment/DOMAIN-SPEC.md`
- `02-domain/payment/PROVIDER-SPEC.md`
- `08-tests/payment.md`
- `09-operations/payment-runbook.md`
- `tasks/TASK-015.md`


## V6.4 提醒中心 / 发票邮件
- `alop-notification` 正式成为独立 Domain，统一站内信、短信、邮件。
- 合同到期/续租、账单、逾期、Reservation、支付退款、CRM/工单等提醒通过业务事件驱动。
- NotificationRule 决定收件人、短信/邮件渠道、静默时段、Fallback、模板和幂等。
- 发票 `ISSUED` 后支持自动邮件发送，支持 PDF/OFD 附件、发送历史、手工重发、失败重试和 Bounce 处理。
- 发票邮件失败不会回滚发票税务状态；达到重试上限进入 IntegrationTask。
- 新增 `TASK-025`、Notification Flyway/OpenAPI、Invoice Delivery DDL/API/Event/Test/Runbook。

## V7.0 Enterprise Operations Hardening

This revision adds:
- AgreementParty
- SecurityDeposit
- UtilityUsagePeriod
- UnidentifiedCollection
- ResourceTransfer
- Tax Domain
- Accounts Payable
- OwnerSettlement

These capabilities are available as TASK-026 through TASK-033 and are intended to be implemented before freezing the next major code-generation baseline.


## V7.0 Frozen Codegen Baseline
V7.0 no longer expands ordinary business scope. It adds canonical machine-readable codegen contracts, table dictionary, API/event catalogs, transaction/lock/idempotency/job matrices, task dependency graph, canonical test fixtures and release gates.

Start implementation from `tasks/TASK-001.md` and use `11-codegen/TASK-CONTEXT-MATRIX.yaml` to load the exact contract set for each task.
