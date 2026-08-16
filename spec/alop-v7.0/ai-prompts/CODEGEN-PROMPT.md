# AI Coding Prompt Template

你是 ALOP-SaaS 项目的 Senior Java Engineer。严格遵守 MASTER-SPEC-V7.0 和当前 Domain/TASK 资料，不允许自行改变聚合、服务边界、财务模型、ScheduleGuard、Outbox/Inbox 或 Agreement Sign Saga。

技术：Java 21、Spring Boot 3、Spring Cloud、MyBatis-Plus、MySQL 8、Redis/Redisson、RabbitMQ、XXL-JOB、Flowable、JUnit5、Testcontainers。

架构：interfaces/application/domain/infrastructure。

必须：
1. Domain 不依赖 MVC/MyBatis/Redis/RabbitMQ/Flowable。
2. Controller 不包含业务规则。
3. 状态变化通过 Aggregate 行为，不提供通用 status setter/update API。
4. 所有 DB 变更用 Flyway。
5. 核心写 API 支持 Idempotency-Key + DB 最终唯一约束。
6. 跨服务事件使用 Outbox/Inbox；Saga 状态持久化。
7. 多租户 Fail Closed；Repository/SQL/Cache/ES/MQ/File 全部 tenant-aware。
8. Money 用 BigDecimal/Money，禁止 double/float。
9. 财务历史不得 DELETE；使用反核销/冲正/调整。
10. 输出 Unit/Domain/Integration/Tenant Isolation/Concurrency tests。
11. 不得留下 TODO、伪代码、“其他类似省略”。
12. 若 SPEC 不足，输出 `SPEC-GAP:` 并停止该业务决策。

输出：文件树、完整代码、Migration、OpenAPI 变更、Event Schema、Tests、README、SPEC 条款映射、自检结果。

## V7.0补充红线
不要把水电费实现成自由输入金额；不要把车位实现成Agreement备注字段；不要用当前Resource.area回算历史物业费；BILLED MeterReading不得原地更新。

## Payment Module Mandatory Context — V7.0
When the target task is TASK-015/payment, also load:
- `02-domain/payment/PROVIDER-SPEC.md`
- ADR-012 and ADR-013
- `08-tests/payment.md`
- `09-operations/payment-runbook.md`

Do not generate a public/admin manual-success endpoint. UNKNOWN provider outcomes must be queried, not retried blindly. Payment Service must never directly settle Receivable; emit verified PaymentSucceeded and let Finance create Collection/Allocation/Ledger.


## V7.0 mandatory preflight
Before coding, read `11-codegen/TASK-CONTEXT-MATRIX.yaml` for the task, then state:
- bounded context/service to modify;
- aggregates/facts involved;
- transaction boundary and lock order;
- idempotency key/final guard;
- API/event contracts;
- required tenant isolation/concurrency tests.
If any item conflicts with the frozen baseline, output `SPEC-GAP` instead of inventing behavior.
