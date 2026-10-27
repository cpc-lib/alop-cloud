# TASK-003 IAM Platform Authorization Design

## Goal

为平台级操作建立独立于租户 membership/RBAC 的 IAM 授权模型，并为 Tenant v2 创建接口提供一个真实、可追踪、fail-closed 的平台权限查询边界。本设计只解决“当前平台操作者是否拥有 `platform:tenant:create`”，不提前实现租户管理员 provisioning 或 Tenant 创建成功链路。

## Context and constraints

- `tenant_user_membership`、`role`、`user_role` 是租户域模型，当前约束为先有租户 membership 才能分配租户角色。
- DDL 总则允许平台共享数据使用 `tenant_id=0`，但 IAM domain spec 没有把它定义为平台身份；本设计不把 `tenant_id=0` 当作平台 membership。
- Tenant v2 请求中的 `adminUserId` 是目标租户管理员，不是当前 HTTP 操作者，不能参与当前权限判断。
- Gateway/IAM 当前已有 mTLS 内部 membership verification bridge。平台授权复用相同的服务间信任边界，但使用独立 operation、独立查询和独立响应模型。
- 现有 Tenant v2 Controller 是 fail-closed 边界。本设计完成后，授权成功仍返回 `TENANT_CREATE_NOT_READY`，直到管理员 provisioning、Outbox、`TenantCreated` 事件和其余租户初始化具备可靠落点。

## Decisions

### 1. 独立平台授权表

在 IAM 的下一版 Flyway migration 中新增四张表：

| Table | Purpose | Scope key |
| --- | --- | --- |
| `platform_role` | 平台角色目录 | `role_code` globally unique |
| `platform_permission` | 平台权限目录 | `permission_code` globally unique |
| `platform_role_permission` | 平台角色到权限的关系 | `(role_id, permission_id)` |
| `platform_user_role` | 平台用户到平台角色的关系 | `(user_id, role_id)` |

这些表不包含 `tenant_id`，也不引用 `tenant_user_membership`。所有引用的 `user_id` 必须对应 IAM 自己的 `platform_user`；跨服务不得直接写 IAM 数据库。

新表遵循 V7.0 DDL 约定：可变目录/分配表包含 `status`、`version`、`created_at`、`updated_at`、`created_by` 和 `updated_by`；关系表保留主键唯一性并由 IAM 本地事务维护关系完整性。实现不依赖数据库外键，因为现有 IAM V1 DDL 使用应用层关系完整性。

迁移注册 `platform:tenant:create`，并创建一个只包含该权限的受控平台角色模板。迁移不创建默认平台用户，也不自动把任何现有用户绑定到该角色；这样不会引入隐含超级管理员。

### 2. 平台授权是一个原子应用边界

Tenant 应用层使用一个 `PlatformAuthorizationPort` 完成一次请求的身份确认和权限确认：

```text
authorize(bearerToken, permissionCode) -> PlatformActor
```

返回的 `PlatformActor` 只表示 IAM 已确认的用户 ID。将 actor 解析和权限检查合并为一次远程决策，避免先解析身份、再重复查询权限造成 TOCTOU 或两次网络调用。当前 `adminUserId` 永远不进入这个调用。

Tenant Controller 只做 DTO/header 校验并调用该 port；它不读取 IAM 数据库、不解析 JWT claims 作为授权结论，也不从请求体推导 actor。

### 3. 内部 IAM 平台授权 API

新增 mTLS 保护的内部接口：

```text
POST /internal/iam/v1/platform/authorize
Authorization: Bearer <user-access-token>
Content-Type: application/json

{
  "permissionCode": "platform:tenant:create"
}
```

成功响应：

```json
{
  "userId": 1001,
  "permissionCode": "platform:tenant:create"
}
```

处理顺序固定为：

1. 校验 mTLS/service identity；
2. 校验 Bearer token 的签名、issuer、audience、时间窗口和 numeric `sub`；
3. 查询 `platform_user`，要求用户为 `ACTIVE`；
4. 通过 `platform_user_role -> platform_role_permission -> platform_permission` 查询关系，要求角色、权限和分配均为 `ACTIVE`；
5. 返回已确认的 `userId` 和请求权限码。

IAM 不接受 `X-User-ID`、`X-Role`、租户 header 或请求体中的 user ID 作为身份来源。平台 JWT 不要求 `tenantId` 或 `membershipId`，因为平台操作不具备租户 membership 上下文；现有租户 JWT membership bridge 不被复用或放宽。

### 4. Tenant 端 fail-closed 客户端

Tenant 的生产 adapter 通过配置的 IAM HTTPS/mTLS base URL 调用上述 API。配置缺失、证书/连接失败、超时、IAM 5xx、响应结构不完整或权限不匹配均不得降级为本地允许：

| IAM/client situation | Tenant result |
| --- | --- |
| token 缺失、格式错误、IAM 返回 401/403 | `TENANT_ACCESS_DENIED` / HTTP 403 |
| Tenant 的 IAM URL/mTLS 配置缺失，或 IAM 的 JWT verifier 配置缺失 | 对应服务启动失败 |
| IAM timeout、连接失败、5xx、响应解析失败 | `TENANT_CREATE_NOT_READY` / HTTP 503 |
| IAM 返回有效 actor + permission | 进入下一道 Tenant readiness boundary；本 slice 仍返回 `TENANT_CREATE_NOT_READY` |

不使用正向缓存，不在 IAM 不可用时允许平台创建。调用日志只记录 trace、operation、结果和耗时，不记录 Bearer token。

### 5. 不在本 slice 绑定 adminUserId

管理员 provisioning 是后续独立 slice，必须由 IAM-owned command 和 Tenant-owned durable integration state 共同完成：Tenant 本地事务写入创建事实/Outbox，IAM 通过版本化内部命令幂等创建 `tenant_user_membership` 并分配租户管理员角色，失败由持久化 Saga/IntegrationTask 重试。该流程未在本设计中实现，也不允许 Tenant 直接写 IAM 表。

因此本 slice 不把 Tenant v2 Controller 改成 200 成功路径；授权成功后仍保持现有 `TENANT_CREATE_NOT_READY`，直到 provisioning 设计另行批准。

## Data flow

```text
HTTP POST /api/platform/v2/tenants
  -> validate Idempotency-Key + body
  -> PlatformAuthorizationPort.authorize(Authorization, "platform:tenant:create")
       -> mTLS IAM /internal/iam/v1/platform/authorize
       -> verify platform JWT
       -> active platform_user
       -> active platform_user_role
       -> active platform_role_permission
       -> active platform_permission
  -> PlatformActor returned
  -> current Tenant readiness boundary
  -> TENANT_CREATE_NOT_READY (this slice)
```

拒绝路径不触发套餐查询、ID 生成、幂等记录写入或 Tenant 写入；平台授权服务故障也不触发任何 Tenant 写入。

## Error contract

本 slice 复用已有稳定 Tenant 错误码：

- `TENANT_ACCESS_DENIED`：缺少/无效用户 token、用户非 ACTIVE、平台角色/权限关系不存在或非 ACTIVE。
- `TENANT_CREATE_NOT_READY`：IAM transport/configuration 不可用，或授权成功但管理员 provisioning 尚未具备可靠落点。
- `TENANT_INVALID_REQUEST`：Tenant 自身 header/body 校验失败。

IAM 内部接口可以返回内部错误对象，但不得泄露用户是否拥有某个具体角色的细节；未知 permission code 与无权限统一按拒绝处理。

## Files and ownership

实现计划将限定在以下边界：

- IAM migration：`spec/alop-v7.0/03-database/flyway/iam/V2__platform_authorization.sql`；
- IAM internal OpenAPI、API catalog、权限/领域/traceability registry；
- IAM 平台授权 domain/application/repository/controller/config/test；
- Tenant 的 `PlatformAuthorizationPort`、IAM HTTPS/mTLS adapter、配置和 REST boundary tests；
- Test support 的真实 MySQL seed/verification。

不修改冻结的 `spec/alop-v7.0/03-database/flyway/iam/V1__init.sql`、Tenant V1 OpenAPI 或既有 membership verification API。

## Testing strategy

先写失败测试，再写最小实现：

1. 纯 domain/application 测试：ACTIVE 平台用户和完整角色链路允许；禁用用户、禁用角色、禁用权限、缺失映射和未知权限拒绝。
2. IAM repository MySQL 集成测试：跨租户 membership 即使存在也不能替代平台角色；重复关系保持幂等；权限撤销立即生效。
3. IAM internal controller 测试：缺 token、无效 token、拒绝、允许、IAM DB 故障的稳定 HTTP 映射；接口必须要求 mTLS 配置。
4. Tenant client/controller 测试：授权成功仍返回 503 readiness boundary；IAM 403 映射 403；超时/5xx/坏响应映射 503；任何失败都不调用 `CreateTenantService`。
5. 真实 Docker verification：执行 IAM migration、签发测试 RSA JWT、插入平台角色链路，验证真实 MySQL allow/deny；回归现有 IAM membership verification 和 Tenant create 集成测试。
6. 静态门禁：YAML/CSV 解析、项目 POM 校验、`git diff --check`，并确认冻结 V1 文件未变更。

## Non-goals

- 不实现 Tenant admin membership provisioning；
- 不新增 Provisioning Saga、Outbox 或 `TenantCreated` 事件注册；
- 不修改 Tenant 状态机，不开放 v2 200 创建成功响应；
- 不实现平台角色管理 UI、平台用户管理 UI、DataScope、Resource ACL 或 SupportSession；
- 不创建默认管理员、默认用户或隐式超级管理员；
- 不把 `tenant_id=0` 用作平台授权身份；
- 不放宽现有租户 JWT membership verification 的必填 claims 或 fail-closed 语义。

## Success criteria

本 slice 完成后：

1. 平台授权的唯一数据来源是 IAM 新增的独立平台角色/权限链路和 ACTIVE `platform_user`。
2. Tenant 不访问 IAM 数据库，不信任请求体或自定义身份 header，不在 IAM 故障时放行。
3. `platform:tenant:create` 的允许/拒绝行为有真实 MySQL 集成证据和 REST boundary 测试。
4. Tenant v2 在授权成功但 provisioning 尚未完成时仍明确返回 `TENANT_CREATE_NOT_READY`，没有虚假的 200 成功。
5. 冻结的 IAM V1 DDL、Tenant V1 API/DDL 和既有 membership verification contract 保持不变。
