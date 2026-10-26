# TASK-001 JWT/IAM/Gateway 权威核验桥接设计

**状态：待评审**

**范围：TASK-001 Platform Foundation 的安全闭环切片**

**日期：2026-10-25**

## 1. 目标

在不绕过 V7.0 冻结边界的前提下，完成 Gateway 与 IAM 之间的最小真实安全闭环：Gateway 验证访问令牌后，通过受保护的内部调用请求 IAM 实时核验当前用户与租户 Membership；只有 IAM 返回当前仍有效的 Membership 身份时才放行请求。

本切片不宣称完成 TASK-001 全部范围，不进入 TASK-002 Tenant Core，也不实现登录、刷新、RBAC、DataScope、ResourceACL 或业务域。

## 2. 当前依据与约束

- V7.0 的 TenantContext 必须来自认证后的 Membership；客户端 `X-Tenant-Id` 只能作为选择器，不能作为身份来源。
- IAM 拥有 `platform_user` 与 `tenant_user_membership`，Gateway 不得访问 IAM 数据库。
- 当前 Gateway 已有 `GatewayMembershipAuthority` SPI、HTTPS JWKS 启动保护和 JWT 基础校验，但没有真实 IAM bridge 实现。
- 当前 `JdbcMembershipAuthority` 已能实时校验 user、membership、tenant 三者关系及 ACTIVE 状态。
- `tenant` 生命周期由 `alop-tenant` 拥有；TASK-001 不跨库读取 Tenant 表。Tenant ACTIVE/SUSPENDED/TERMINATED 的最终准入属于 TASK-002 依赖。
- `SPEC-GAP-TASK-001-002` 仍需通过 ADR/内部契约流程确认。未确认前不得直接修改冻结公共 OpenAPI、DDL、事件或任务追踪契约。

## 3. 方案与取舍

### 3.1 采用方案：IAM 内部权威核验接口 + Gateway 适配器

调用链：

```text
Client
  -> Gateway
     本地验证 Bearer JWT + X-Tenant-Id
  -> IAM internal verification endpoint over mTLS
     IAM 重新验签并实时查询 IAM 自有 membership 数据
  -> Gateway
     将最小可信 TenantContext 放入 ServerWebExchange attribute 后放行
```

Gateway 不访问 IAM 数据库、不把客户端身份头提升为可信身份，也不向下游生成可替代独立鉴权的 `X-User-Id` 或 `X-Membership-Id`。下游服务仍须独立验证 Bearer、Membership、权限和数据范围。

### 3.2 被否决的方案

1. **OIDC token introspection**：协议标准化程度较高，但会引入与当前 JWT claim + live membership 模型不同的响应语义和额外映射；当前切片不需要这层抽象。
2. **仅使用 Gateway 本地适配器或直接查 IAM DB**：前者不能形成真实跨服务信任闭环，后者违反服务边界与 IAM 数据所有权，均不能作为生产方案。

## 4. 内部接口契约草案

此接口是内部契约草案，不是当前 V7.0 公共 API。确认后应在 ADR-002 和内部 OpenAPI/schema 中冻结，再接入任务追踪。

### 4.1 请求

```text
POST /internal/iam/v1/membership/verify

mTLS client identity: alop-gateway
Authorization: Bearer <原始 access token>
X-Selected-Tenant-Id: <正整数>
traceparent / X-Request-Id: 仅用于追踪，不参与授权
```

请求不使用 JSON body，避免把用户令牌复制到业务对象或普通请求日志中。原始 access token 只在受 mTLS 保护的 Authorization header 中传递；不得写入日志。

### 4.2 成功响应

```json
{
  "userId": 10001,
  "tenantId": 20001,
  "membershipId": 30001
}
```

响应只返回形成可信 TenantContext 所需的最小身份，不返回角色、权限、DataScope 或任何业务数据。

### 4.3 失败语义

| IAM 结果 | 语义 | Gateway 结果 |
| --- | --- | --- |
| `200` | JWT 有效，且 user、membership、tenant 全匹配并为 ACTIVE | 放行 |
| `401 TENANT_CONTEXT_REQUIRED` | token 缺失、格式错误、签名/issuer/audience/时间/必要 claim 无效 | `401` |
| `403 TENANT_ACCESS_DENIED` | token tenant 与选择器不一致，或 user/membership 非 ACTIVE/不匹配 | `403` |
| `5xx`、连接失败、超时 | IAM 或其数据库不可用 | `503`，拒绝放行 |
| Gateway 本地并发额度耗尽 | 无法安全发起核验 | `503`，拒绝放行 |

IAM 必须使用自己的受信 `JwtDecoder` 重新验证原始 token，不能相信 Gateway 传来的 user、membership 或解码结果。IAM 不可用时不得使用正向缓存放行。

## 5. 组件职责与拟变更边界

### IAM

- `JwtMembershipAuthenticator`：复用并收敛 issuer、audience、RS256、时间窗口和必需 claim 校验。
- `MembershipVerificationService`：组合 JWT 验证与 `JdbcMembershipAuthority` 实时查询。
- `MembershipVerificationController`：读取内部 header、调用应用服务、映射稳定错误码和 HTTP 状态。
- 内部认证配置：只允许 HTTPS JWKS，issuer/audience 必须显式配置；服务证书由 TLS/部署层配置，密钥只引用外部 Secret/KeyStore。
- 不新增表、不改 IAM 冻结 DDL、不在本切片返回角色权限。

### Gateway

- `IamMembershipAuthorityClient`：使用 `WebClient` 实现现有 `GatewayMembershipAuthority` SPI。
- `GatewayTenantSecurityConfiguration`：创建 IAM client，并强制 HTTPS/mTLS 配置完整。
- `GatewayJwtTenantFilter`：保留本地 JWT 防线，明确映射 IAM 401/403/超时/5xx，继续清理伪造身份 header。
- 显式路由策略：公开路径必须逐项配置；未配置的绕过默认拒绝，避免把健康检查、登录、支付回调混成通配白名单。

### 不在本切片内

- 登录、刷新、注销、JWKS 发布和密钥轮换完整实现；
- RBAC、DataScope、ResourceACL 和服务侧完整二次鉴权；
- Tenant Core 生命周期查询；
- 业务服务实现、数据库迁移、支付/财务逻辑；
- 生产证书签发、网络策略编排和完整部署自动化。

## 6. 超时、并发与可观测性

- Gateway→IAM 不做自动重试，避免认证请求放大和重复负载。
- 核验响应超时默认 500ms；只能通过受约束配置调整，不能取消上限。
- Gateway 对在途核验设置默认最大并发 128；额度耗尽立即拒绝，不排队等待。
- 指标记录结果类别、延迟、超时、IAM 错误和本地并发拒绝；不得使用完整 token、密码、证书私钥或高基数 token 内容作为标签。
- 认证失败和 IAM 不可用需要携带 trace/request correlation，但日志只记录不可逆的请求关联信息。

本切片不额外引入未经项目统一规范确认的复杂重试或熔断库；正确性依赖“超时 + 有界并发 + fail closed”。熔断可作为后续运行治理增强，但不能改变拒绝放行语义。

## 7. 验收与证据

### Gateway 单测

- 有效 JWT + IAM 返回有效 TenantContext：请求放行；
- JWT 无效：返回 401，且不调用 IAM；
- IAM 返回 401：返回 401；
- IAM 返回 403：返回 403；
- IAM 超时、连接失败、5xx：返回 503；
- 并发额度耗尽：立即返回 503；
- 不自动重试，伪造 `X-User-Id`、`X-Membership-Id`、`X-Alop-*` 不会透传为可信身份。

### IAM Web/应用测试

- 有效签名 token + ACTIVE user + ACTIVE membership：返回最小身份；
- 签名、issuer、audience、时间窗口、RS256 或必要 claim 无效：返回 401；
- user/Membership 非 ACTIVE、claim 与选择租户不匹配或关联关系不一致：返回 403；
- IAM 数据库异常：返回 5xx，Gateway 映射为 503；
- 不把 token 放入响应或日志。

### MySQL/跨服务测试

- 租户 A/B 隔离：A 的 token 不能核验 B 的选择器或 membership；
- 禁用 user、禁用 membership 均不能继续放行；
- 不同 JWT claim 与实时数据库状态冲突时，以 IAM 实时状态拒绝；
- 真实 Gateway client 与 IAM endpoint 的请求/响应字段和状态码符合契约。

### 回归命令

本轮实现后重新执行：

```powershell
python scripts/validate_project.py
python scripts/verify_rabbit_mysql_docker.py
cd backend
mvn -pl alop-gateway -am test -B
mvn -pl alop-iam/alop-iam-server -am test -B
```

历史 `HANDOFF.md` 和 `PROGRESS.md` 中的通过记录只能作为基线，不能替代本轮命令输出。

## 8. SPEC-GAP 与完成定义

本设计保留 `SPEC-GAP-TASK-001-002`，直到以下内容被明确批准并登记：

1. JWT claim 类型、issuer/audience、最大 TTL、JWKS `kid`/轮换与撤销语义；
2. 内部核验接口路径、认证方式、请求 header、响应 schema 和错误映射；
3. 公开/内部路由豁免清单及其安全边界；
4. Gateway/IAM 的部署网络与 mTLS 身份配置。

本切片只有在代码、单测、MySQL/跨服务测试、契约校验和回归命令都有本轮证据后，才能把“安全桥接切片完成”写入进度台账；不能把它表述为 TASK-001 完成、全域租户隔离完成或 V7.0 生产就绪。
