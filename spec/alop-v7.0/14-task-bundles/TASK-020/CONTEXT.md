# TASK-020 Codegen Context Bundle

Title: TASK-020 - React Admin Web (Frontend Only)

Dependencies: TASK-019

## Goal
基于 `04-openapi/*.yaml` 全部 admin 端点契约构建管理后台前端 `alop-admin-web`。本任务为纯前端任务：不修改后端服务、数据库 schema、Flyway migration，不涉及锁序与幂等守卫。

## Mandatory context files
- `00-master/MASTER-SPEC-V7.0.md`
- `00-master/V7-FREEZE-POLICY.md`
- `01-architecture/architecture-baseline.md`
- `11-codegen/CODING-STANDARDS.md`
- `11-codegen/API-CATALOG.yaml`
- `07-security/tenant-isolation.md`
- `07-security/permissions.md`
- `10-registries/error-codes.yaml`
- `10-registries/permissions.yaml`
- `10-registries/dictionaries.yaml`
- `13-acceptance/RELEASE-GATES.md`
- `13-acceptance/MODULE-DOD-MATRIX.md`
- `tasks/TASK-020.md`
- `docs/architecture/SERVICE-BOUNDARIES.md`
- `04-openapi/agreement.yaml`
- `04-openapi/agreement-extensions.yaml`
- `04-openapi/ap.yaml`
- `04-openapi/asset.yaml`
- `04-openapi/billing.yaml`
- `04-openapi/crm.yaml`
- `04-openapi/finance.yaml`
- `04-openapi/finance-extensions.yaml`
- `04-openapi/invoice.yaml`
- `04-openapi/notification.yaml`
- `04-openapi/owner-settlement.yaml`
- `04-openapi/payment.yaml`
- `04-openapi/tax.yaml`
- `04-openapi/tenant.yaml`
- `04-openapi/utility-parking.yaml`

## Tech baseline
- React + TypeScript SPA，代码落在 `alop-admin-web/`。
- 路由权限模型以 `10-registries/permissions.yaml` 为唯一事实源：路由守卫、菜单可见性、按钮级操作均按 permission code 控制。
- 服务边界、服务名与 API 前缀参照 `docs/architecture/SERVICE-BOUNDARIES.md`。

## Feature module checklist
对照 `alop-admin-web/src/features/` 的 feature 目录逐一实现（每个 feature 至少列表页 + 详情页）：
- `agreement` - 合同管理
- `ap` - 应付管理
- `asset` - 资产与资源
- `billing` - 账单计费
- `crm` - 客户与线索
- `finance` - 财务核心
- `iam` - 身份与权限
- `integration` - 审批流/集成平台
- `invoice` - 发票
- `notification` - 通知中心
- `operations` - 运营与交房
- `organization` - 组织架构
- `owner-settlement` - 业主结算
- `payment` - 支付
- `reservation` - 预约锁位
- `search` - 360 检索
- `tax` - 税务
- `tenant` - 租户管理

## API layer rules
- 从 `11-codegen/API-CATALOG.yaml` 生成 typed client（openapi-typescript 或等价工具链）；禁止手写接口 URL 与请求/响应类型。
- 错误码映射统一走 `10-registries/error-codes.yaml`，字典枚举统一走 `10-registries/dictionaries.yaml`。

## Permission routing
- 基于 `07-security/permissions.md` 实现路由级与操作级权限：未授权路由重定向/403，未授权操作按钮隐藏或禁用。

## Agent preflight output
Before generating code, state:
1. feature module and pages to modify;
2. API endpoints consumed (traced to `11-codegen/API-CATALOG.yaml` and `04-openapi/*.yaml`);
3. permission codes guarding routes and actions;
4. state management, typed client, and mock strategy;
5. any `SPEC-GAP` found.

## DoD
- build passes (type-check + production build);
- lint passes;
- every feature directory has at least list + detail pages;
- permission control effective (route guard + action-level control verified);
- mock-integrated pages verified against `04-openapi/*.yaml` contracts.

## Completion rule
Generated code is not complete until the task DoD and V7 release gates relevant to this module pass.
