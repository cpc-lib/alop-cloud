# Tenant v2 Provisioning Saga Design

## Goal

将 Tenant v2 创建从当前的 `TENANT_CREATE_NOT_READY` 边界推进到一个可审计、可重试、幂等的创建闭环：平台操作者通过 IAM 授权后，Tenant 创建 `CREATING` 租户，可靠地请求 IAM 绑定管理员，完成租户基础资源初始化，最后才把租户转为 `ACTIVE` 并发布正式的 `TenantCreated` 事件。

本设计只允许 `adminUserId` 绑定已经存在且有效的 IAM `platform_user`；不创建新用户、不发送邀请、不把平台用户自动提升为平台超级管理员。

## Context and constraints

- TASK-003 已建立独立的平台角色/权限链路，Tenant v2 当前只完成平台授权，授权成功仍返回 `TENANT_CREATE_NOT_READY`。
- Tenant V2 已有 `tenant_create_request_id` 唯一约束和平台作用域幂等记录，但 `CreateTenantService` 尚未写入 Outbox，也没有调用 IAM provisioning。
- Tenant 生命周期已有 `CREATING -> ACTIVE` 的领域转换；业务流量只允许进入 `ACTIVE` 租户。
- `mq_outbox`、`mq_inbox`、`integration_task` 是既有的 V7.0 集成基础设施，要求本地事务和至少一次投递。
- 当前 `TenantContext` 表达“已认证租户成员”，新租户在 provisioning 开始时尚无成员，不能用伪造的 membership/user ID 作为后台上下文。
- `tenant.tenant.created.v1` 在 V7.0 当前注册表中仍标记为 V6 legacy name，不能直接按旧注册表状态使用；本设计将其作为新的正式 V7.0 合约注册。
- `support_session` 是平台支持访问能力，不属于 CreateTenant 的必要步骤，本设计不把它混入租户创建 Saga。

## Decisions

### 1. 使用 Tenant-owned Saga + Outbox/Inbox，而不是跨服务同步事务

采用事件驱动的持久化 Saga：

```text
Tenant HTTP request
  -> Tenant local transaction: tenant(CREATING) + idempotency + provisioning Outbox
  -> RabbitMQ at-least-once delivery
  -> IAM Inbox + tenant membership + TENANT_ADMIN role/permissions
  -> IAM completion/failure Outbox
  -> Tenant Inbox + baseline initialization + lifecycle transition
  -> TenantCreated Outbox
```

不采用 Tenant 到 IAM 的同步写入或同步跨服务事务。同步调用无法把 Tenant 本地提交、IAM 提交和 RabbitMQ 发布绑定为一个原子操作，超时后容易出现“已创建但客户端认为失败”或重复授权；Outbox/Inbox 能把这些状态保留为可查询、可重试的事实。

### 2. 创建请求的本地事务和 HTTP 语义

`TenantCreateV2Controller` 在平台授权成功后，构造包含以下字段的 `CreateTenantCommand`：

- `idempotencyKey`
- `tenantCreateRequestId`
- `tenantCode`
- `tenantName`
- `tenantType`
- `packageCode`
- `adminUserId`
- `timezone`
- `currency`

这些字段全部参与规范化请求哈希；同一个幂等键提交不同管理员、时区或币种必须返回 `IDEMPOTENCY_KEY_CONFLICT`。

同一 JDBC 事务完成：

1. 插入或锁定现有平台作用域幂等记录；
2. 解析 ACTIVE `tenant_package`；
3. 插入 `CREATING` 状态的 Tenant；
4. 写入 `tenant.provisioning.requested.v1` Outbox；
5. 将幂等响应固化为 `202 Accepted` 语义的 `CREATING` 结果；
6. 提交事务。

重复请求返回第一次创建的相同结果，不重复生成 Tenant、Outbox 或 IAM provisioning。租户最终状态通过 Tenant 查询能力获取；创建响应不把 `CREATING` 伪装成 `ACTIVE`。

若平台授权、参数校验、套餐解析、Tenant 写入或 Outbox 写入任一失败，整个本地事务回滚，且不产生可投递的 provisioning 请求。

### 3. 事件合约

新增并注册以下 V7.0 事件。所有事件使用现有 envelope 规则、`eventVersion: 1.0`、正数 `tenantId`、trace/correlation 字段和明确的 `data` 对象。

| Event type | Producer | Consumer | Purpose |
| --- | --- | --- | --- |
| `tenant.provisioning.requested.v1` | `alop-tenant` | `alop-iam` | 请求为目标租户绑定既有平台用户并创建租户管理员授权 |
| `iam.tenant.provisioning.completed.v1` | `alop-iam` | `alop-tenant` | IAM 成员、角色和权限初始化完成 |
| `iam.tenant.provisioning.failed.v1` | `alop-iam` | `alop-tenant` | IAM provisioning 不可重试或达到失败边界 |
| `tenant.tenant.created.v1` | `alop-tenant` | 当前无业务消费者，保留注册扩展点 | 租户所有必要创建步骤完成并已转为 ACTIVE |

`tenant.provisioning.requested.v1.data` 至少包含：`tenantId`、`tenantCreateRequestId`、`adminUserId`、`tenantPackageId`、`timezone`、`currency`。

`iam.tenant.provisioning.completed.v1.data` 至少包含：`tenantId`、`adminUserId`、`membershipId`、`adminRoleId`、`tenantCreateRequestId`。

`iam.tenant.provisioning.failed.v1.data` 至少包含：`tenantId`、`adminUserId`、`tenantCreateRequestId`、稳定 `errorCode`、可安全展示的 `retryable` 标志。不得把用户是否存在、角色关系或内部 SQL 细节泄露到外部 API。

`tenant.tenant.created.v1.data` 至少包含：`tenantId`、`tenantCreateRequestId`、`tenantCode`、`tenantName`、`tenantType`、`packageId`、`adminUserId`、`timezone`、`currency`。

事件 schema 文件和 `event-registry.yaml`、`registry.md` 必须同步更新。旧的 V6 名称不作为别名继续发布。

### 4. IAM provisioning 的事务边界和权限边界

IAM consumer 使用 Inbox 和一条本地 JDBC 事务完成：

1. 校验事件 envelope、`tenantId`、`adminUserId` 和请求关联字段；
2. 查询 `platform_user`，要求用户存在且为 `ACTIVE`；
3. 按 `(tenant_id, user_id)` 幂等创建或复用 `tenant_user_membership`；
4. 按 `(tenant_id, role_code)` 幂等创建或复用 `TENANT_ADMIN` 角色；只有现有角色的 ACTIVE 权限集合已经完全一致时才允许复用；
5. 确保新建或复用的角色只绑定以下三项权限：
   - `tenant:user:manage`
   - `tenant:role:manage`
   - `organization:team:manage`
6. 按 `(tenant_id, user_id, role_id)` 幂等创建用户角色关系；
7. 写入 `iam.tenant.provisioning.completed.v1` Outbox；
8. 将 Inbox 标记为 `PROCESSED` 并提交。

若同一租户已有 `TENANT_ADMIN` 但绑定了额外权限，IAM 不删除或覆盖现有关系，而是返回稳定的 `TENANT_ADMIN_ROLE_CONFLICT` 失败并创建人工任务；这样不会因 provisioning 重试意外影响已有授权。不授予资产、财务、支付等业务权限，不创建隐含的 superadmin 角色，不把平台角色表和租户角色表混用。所有重复投递必须在同一 Inbox/业务事务边界内安全重放。

`TENANT_ADMIN` 的角色 code/name 作为 IAM 领域内的受控默认模板（角色 code `TENANT_ADMIN`，名称“租户管理员”）；它不是平台角色，也不允许通过此次 provisioning 自动绑定平台权限。

### 5. Tenant 基础初始化策略

Tenant 收到 `iam.tenant.provisioning.completed.v1` 后，在本地 Inbox 事务中完成必要基础资源初始化。初始化策略不把业务默认值散落在代码中，而是由明确的 `TenantProvisioningPolicy` 提供：

- `tenant_quota`：创建一条租户配额记录；未被套餐策略覆盖的字段使用现有 DDL 默认值。
- `tenant` canonical fields：将请求的 `timezone` 和 `currency` 写入租户主记录；缺省值沿用现有 V7.0 约定。
- `tenant_feature`：只为套餐 capability 明确列出的 feature 创建初始记录；未声明的 feature 不隐式开启。
- `tenant_route`：必须由 provisioning 配置提供 `route_type`、`datasource_key`、region/namespace 策略；缺失配置时 provisioning 失败，不写入猜测值。
- `tenant_branding`：不作为 CreateTenant 的 ACTIVE 前置条件，本轮不创建空壳 branding 记录。
- `support_session`：不属于本 Saga，保持独立命令和权限边界。

所有上述 Tenant 本地写入与 `Tenant.provisionSucceeded()`、`tenant.tenant.created.v1` Outbox 处于同一事务。只有必要初始化全部成功，Tenant 才能从 `CREATING` 转为 `ACTIVE`。

### 6. 内部服务执行上下文

不能通过构造不存在的 `TenantContext(tenantId, membershipId, userId)` 来让新租户 provisioning 工作。

在集成框架中增加最小的可信内部执行路径：

- 用户请求仍只能使用现有的已认证 `TenantContext`；
- Outbox/InBox 的内部 provisioning 方法接收经过配置认证的 service identity 或专用 `InternalEventContext`；
- 内部上下文不能被 HTTP header、JWT tenant claim 或普通业务代码构造；
- 事件中的 `tenantId` 仍必须是正数，并用于数据库隔离、Inbox 唯一键和 Outbox envelope；
- 内部 consumer 只能访问其明确拥有的 provisioning 表和操作，不放宽普通租户 API 的数据范围。

这条路径是为了表达“服务间可信后台操作”，不是把平台用户或虚构 membership 当成租户身份。

### 7. 失败、重试和补偿

- IAM 找不到 `adminUserId`、用户非 ACTIVE 或事件字段不合法：写入稳定失败码，产生 `iam.tenant.provisioning.failed.v1`，Tenant 保持 `CREATING`，对应 `integration_task` 标记为人工关注。
- IAM 数据库暂时不可用、死锁、锁等待或 Outbox 发布暂时失败：遵循现有 Outbox/InBox 重试策略，不发送业务失败事件，直到超过重试边界。
- Tenant 初始化临时失败：Inbox 事务回滚，租户仍为 `CREATING`，由消息重投或 integration task 重试。
- Outbox 达到 DEAD：必须和可操作的 `integration_task` 同事务落盘；禁止出现“消息 DEAD 但无任务”的孤儿状态。
- 不做跨服务回滚删除：已经创建的 membership/role/permission 关系由幂等 provisioning 保留；后续人工重试只补齐缺失关系，不重复授权。
- 租户永远不会因为收到失败事件而转为 `ACTIVE`；重复 completion 事件不能重复递增 Tenant version 或重复发布 `TenantCreated`。

### 8. DDL 和注册表变更

只增加版本化迁移和合约文件，不修改已经冻结的 IAM V1、Tenant V1 或 integration V1：

- IAM：如现有表结构不足，新增最小的 provisioning 关联/状态字段迁移；优先复用现有 `tenant_user_membership`、`role`、`permission`、`role_permission`、`user_role` 表和唯一约束。
- Tenant：如 `timezone`/`currency` 需要显式写入，扩展现有 repository/domain 映射，不重写 Tenant V1 DDL。
- Events：增加四个 JSON schema，并更新 `event-registry.yaml` 与 `registry.md`。
- Integration task：只复用既有表，不另造一张 Saga 表；`task_type`、`business_type`、`business_id` 和 `event_id` 必须能定位到 Tenant provisioning。

迁移不得创建默认平台用户、不得把任意现有用户自动授予平台权限，也不得使用 `tenant_id=0` 表示一个真实租户或真实身份。现有幂等记录的 `(tenant_id=0,user_id=0,api_code=TENANT_CREATE_V2)` 仅作为平台 API 的技术作用域，不能被当作身份模型。

## State and data flow

```text
POST /api/platform/v2/tenants
  -> validate request
  -> IAM platform authorization: platform:tenant:create
  -> Tenant local tx:
       idempotency record
       tenant = CREATING
       tenant.provisioning.requested.v1 Outbox
       accepted response
  -> IAM Inbox tx:
       active platform_user check
       tenant membership
       TENANT_ADMIN + exactly 3 tenant permissions
       iam.tenant.provisioning.completed/failed.v1 Outbox
  -> Tenant Inbox tx:
       quota/config/feature/route baseline
       CREATING -> ACTIVE
       tenant.tenant.created.v1 Outbox
```

Lifecycle rules:

- `CREATING` 只能由 Tenant provisioning completion 事务转换为 `ACTIVE`。
- 只有 `ACTIVE` 才允许业务 API；`CREATING` 只允许状态查询、可靠重试和内部 provisioning 回调。
- completion 事件必须关联同一 `tenantCreateRequestId`；不匹配的事件进入失败任务，不更新 Tenant。
- Tenant version 采用现有乐观锁，状态转换必须 `FOR UPDATE` 或等价的版本条件更新。

## API and error contract

新增成功响应模型应明确表示异步创建：

```json
{
  "tenantId": 10001,
  "tenantCreateRequestId": "request-001",
  "status": "CREATING",
  "version": 0
}
```

HTTP 状态为 `202 Accepted`。重复相同幂等请求返回相同 body；不同请求体使用相同幂等键返回 `IDEMPOTENCY_KEY_CONFLICT`。

错误边界：

- 参数校验失败：`TENANT_INVALID_REQUEST` / 400；
- 平台授权拒绝：`TENANT_ACCESS_DENIED` / 403；
- Tenant 本地业务冲突：复用已有稳定错误码；
- IAM/消息基础设施不可用但本地创建尚未提交：`TENANT_CREATE_NOT_READY` / 503；
- 本地创建已经接受后，不把异步 provisioning 失败伪装成同步 500；状态和 integration task 负责表达失败。

## Files and ownership

实现计划应限定在以下边界：

- `alop-tenant`：CreateTenant command/result/service、Tenant lifecycle repository、provisioning policy、Outbox producer、Inbox consumer、REST response/controller、配置和测试；
- `alop-iam`：租户 provisioning consumer、membership/role repository、默认租户管理员角色绑定、completion/failure producer、配置和测试；
- `alop-framework`：只增加受控的内部 event context/Outbox/Inbox 执行入口和对应单元测试；
- `spec/alop-v7.0`：事件 registry/schema、必要的增量 migration、API/traceability 更新；
- `alop-test-support` 与 Docker runner：真实 MySQL/RabbitMQ migration、重复投递、失败重试和跨服务状态验证；
- `docs/development/implementation` 与 `docs/project-management/PROGRESS.md`：记录实现证据和进度。

不修改冻结的 integration V1、IAM V1、Tenant V1 DDL，也不把 Tenant 直接连接到 IAM 数据库执行写入。

## Testing strategy

先写失败测试，再写最小实现：

1. Command/domain 测试：请求哈希包含 `adminUserId`、timezone、currency；`CREATING -> ACTIVE` 只允许一次；错误状态转换拒绝。
2. Tenant service MySQL 测试：Tenant、幂等记录和 provisioning Outbox 同事务；失败全部回滚；相同幂等键不重复创建。
3. IAM provisioning 测试：不存在/禁用 platform user 拒绝；重复事件不重复 membership、role 或 user-role；角色恰好只拥有三项批准权限；不能获得业务权限或平台权限。
4. Tenant completion 测试：只接受匹配的 request ID；重复 completion 不重复初始化、不重复递增 version、不重复 TenantCreated；route policy 缺失保持 CREATING 并生成任务。
5. Framework context 测试：普通用户 TenantContext 不能伪造；内部 provisioning context 仅允许受控调用；tenantId 隔离和 Inbox 唯一键仍生效。
6. REST 测试：授权成功返回 202/CREATING；授权失败仍为 403；IAM 不可用时不创建 Tenant；重复请求返回稳定幂等结果。
7. 真实 Docker 验证：执行所有迁移，插入 ACTIVE platform user，发送 provisioning event，验证 MySQL 中 membership/role/permission、Tenant 状态和 `TenantCreated`；重复发送、临时失败、Outbox retry/dead 都要有证据。
8. 静态门禁：YAML/CSV/JSON schema 解析、API catalog/traceability 校验、`git diff --check`，并确认冻结文件未变更。

## Non-goals

- 不创建新平台用户、不发送邀请、不实现用户注册；
- 不修改平台授权模型或放宽平台 JWT/mTLS fail-closed 语义；
- 不授予租户管理员资产、财务、支付等额外权限；
- 不实现平台角色管理 UI、租户角色管理 UI、DataScope、Resource ACL；
- 不把 `support_session` 纳入租户创建 Saga；
- 不引入 Flowable 或新的工作流引擎；
- 不实现跨服务数据库事务、补偿删除或隐式超级管理员；
- 不把通用 `tenant_config` key/value 体系扩展成未注册的业务配置目录；
- 不声称整个项目已经达到生产发布条件；本设计只关闭 Tenant v2 创建链路的一个明确阻塞面。

## Success criteria

1. Tenant v2 授权成功后返回可重放的 `202 Accepted`，Tenant 进入 `CREATING`，而不是继续返回固定 `TENANT_CREATE_NOT_READY`。
2. `adminUserId` 只能绑定既有 ACTIVE `platform_user`；用户、membership、角色和权限关系均由 IAM 本地事务幂等维护。
3. `TENANT_ADMIN` 只拥有三项批准的租户/组织权限，没有资产、财务、支付或平台超级权限。
4. 所有跨服务效果通过 Outbox/Inbox/Saga 持久化；重复事件、发布重试和失败任务不会产生重复授权或错误 ACTIVE。
5. 只有基础初始化和 IAM completion 全部成功，Tenant 才进入 `ACTIVE` 并发布正式 V7 `tenant.tenant.created.v1`。
6. 真实 MySQL/RabbitMQ 集成测试和静态门禁通过，冻结的 V1 合约没有被修改。
