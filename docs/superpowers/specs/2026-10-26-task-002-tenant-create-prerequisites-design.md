# Tenant Create Prerequisites Design

> Scope amendment: the current Tenant schema and service ownership do not provide a durable admin-user binding. This slice therefore hardens the prerequisites and REST fail-closed boundary, but does not expose a successful v2 create path until IAM provisioning is available.

## Goal

为 Tenant v2 创建租户接口补齐安全、数据和运行时装配前提，使创建流程能够接收 `packageCode`、解析有效套餐、校验平台创建权限，并使用生产级 ID 生成器；在没有可信操作者身份时必须 fail-closed。

## Context

当前 `CreateTenantService` 已实现基于显式请求身份的幂等和租户落库，但命令仍直接接收 `packageId`，服务没有 Spring 生产 Bean，也没有平台操作者与创建权限的应用边界。Tenant v2 OpenAPI 接收的是 `packageCode` 和目标 `adminUserId`，因此直接增加 Controller 会造成字段丢失或把请求体字段误当作当前操作者身份。

## Decisions

### 1. Package code resolves inside the create transaction

新增 `TenantPackageRepository` Port 与 JDBC 实现。创建服务使用 `packageCode` 查询 `tenant_package` 中状态为 `ACTIVE` 的套餐，并将解析出的 ID 传给租户聚合。查询发生在创建事务内；不可用或不存在的套餐统一返回 `TENANT_PACKAGE_NOT_FOUND`。V1 合约、套餐表结构和套餐管理范围不变。

### 2. Actor and authorization are separate from the request body

新增以下应用边界：

- `PlatformActor`：表示已经由上游认证确认的当前平台用户。
- `PlatformActorResolver`：从可信安全上下文解析当前操作者。
- `TenantCreateAuthorizer`：检查操作者是否拥有 `platform:tenant:create`。

`adminUserId` 仅表示新租户的目标管理员，不参与当前操作者身份判断。当前项目尚未提供可信平台身份上下文，因此生产默认实现拒绝缺少身份或权限的请求，并返回 `TENANT_ACCESS_DENIED`；不接受请求体、普通 Header 或默认用户作为身份来源。

### 3. Production ID generation is explicit and fail-fast

新增 Tenant 模块的 Spring 配置类，装配现有 `SnowflakeIdGenerator` 以及租户 ID、幂等记录 ID 的 `LongSupplier`。worker id 使用必填配置 `alop.tenant.worker-id`。缺少配置时应用启动失败，避免多实例使用隐含或重复的 worker id。数据源使用 Spring Boot 标准 `spring.datasource.*` 配置，不在代码内伪造连接信息。

### 4. Create service remains the transaction boundary

`CreateTenantService` 继续持有调用方数据库连接并负责提交、回滚和幂等 replay。套餐解析、幂等记录、租户插入和响应完成记录共享同一事务。已有的重复幂等键、请求身份冲突和数据库唯一键冲突语义保持不变。

### 5. REST v2 is a fail-closed boundary for this slice

为 `POST /api/platform/v2/tenants` 准备请求/错误映射和 Controller 边界测试，但本 slice 不开放一个会返回成功的生产创建链路。Controller 仅负责 header/body 映射、actor 解析和错误映射；不生成 ID、不解析套餐、不直接访问数据库，也不从 `adminUserId` 推导 actor。由于 `adminUserId` 当前没有可靠持久化或 IAM provisioning 落点，生产链路在前置能力未完成时 fail-closed，不宣称 v2 创建成功。

## Data flow

```text
HTTP request
  -> v2 DTO validation
  -> PlatformActorResolver
  -> TenantCreateAuthorizer
  -> fail-closed readiness boundary
  -> CreateTenantService (only after admin provisioning is available)
       -> idempotency insert/replay
       -> active package lookup by packageCode
       -> Tenant.creating(..., resolvedPackageId)
       -> tenant insert
       -> idempotency completion
  -> HTTP error response in this slice
```

所有写入步骤均由 `CreateTenantService` 的单一 JDBC 事务包围。异常时回滚，包括套餐解析成功但租户写入失败的情况。

## Error behavior

| Situation | Error |
| --- | --- |
| 缺少可信平台 actor | `TENANT_ACCESS_DENIED` |
| actor 没有 `platform:tenant:create` | `TENANT_ACCESS_DENIED` |
| adminUserId 没有 IAM provisioning 落点 | 当前 REST 边界拒绝，不返回创建成功 |
| packageCode 不存在或套餐非 ACTIVE | `TENANT_PACKAGE_NOT_FOUND` |
| 同一幂等键对应不同请求 | `IDEMPOTENCY_KEY_CONFLICT` |
| tenantCreateRequestId 或 tenantCode 重复 | `TENANT_CREATE_REQUEST_CONFLICT` |
| worker id 未配置 | 应用启动失败 |

错误码注册表和 `CommonErrors` 同步更新；V1 OpenAPI 与 V1 DDL 保持字节级不变。

## Testing strategy

先写失败测试，再实现最小代码：

1. `TenantPackageRepository` 的 ACTIVE、非 ACTIVE 和不存在行为。
2. `CreateTenantService` 使用 packageCode、拒绝未授权 actor、保留现有幂等和回滚行为。
3. Spring 配置对 worker id 的必填读取、ID supplier 装配和 repository/service Bean 装配。
4. Controller 对 V2 请求映射和 fail-closed 错误映射；不添加虚假的成功响应。
5. 运行 Tenant 模块单测、Maven 编译、项目结构校验，并回归真实 MySQL/RabbitMQ 集成测试。

## Non-goals

本次不实现真实 IAM 权限查询、不引入 Provisioning Saga/Outbox、不增加 TenantPackage 管理接口、不新增管理员绑定表、不修改 V1 合约或已冻结的 V1 数据库定义，也不改变现有租户生命周期状态机。只有在 IAM 成员创建/Provisioning 具备可靠落点后，才开放携带 `adminUserId` 的 v2 成功创建链路。

