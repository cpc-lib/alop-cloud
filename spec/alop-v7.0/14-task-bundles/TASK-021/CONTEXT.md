# TASK-021 Codegen Context Bundle

Title: TASK-021 - UniApp User Client (Frontend Only)

Dependencies: TASK-007, TASK-009, TASK-010, TASK-013, TASK-015, TASK-016, TASK-025

## Goal
构建 UniApp 用户端（小程序/H5）`alop-miniapp`，覆盖 25 个页面。本任务为纯前端任务：不修改后端服务、数据库 schema、Flyway migration，不涉及锁序与幂等守卫。

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
- `tasks/TASK-021.md`
- `docs/architecture/USER-APP-STRUCTURE.md`
- `04-openapi/app-portal.yaml` (new file, being created by a parallel agent)
- `04-openapi/payment.yaml` (`/api/app/v1` endpoints only)

## Tech baseline
- UniApp + Vue 3 + TypeScript，代码落在 `alop-miniapp/`。
- 用户端整体结构（登录态、membership 切换、页面职责划分）以 `docs/architecture/USER-APP-STRUCTURE.md` 为权威。

## Page checklist (25 pages)
对照 `alop-miniapp/src/pages/` 逐页实现：
- `agreement/detail`, `agreement/list`, `agreement/sign`
- `bill/detail`, `bill/list`
- `handover/detail`
- `home/index`
- `invoice/apply`, `invoice/list`
- `login/index`
- `mine/index`, `mine/profile`
- `notification/list`
- `payment/cashier`, `payment/result`
- `quotation/detail`
- `reservation/create`, `reservation/list`
- `resource/detail`, `resource/list`
- `viewing/create`, `viewing/list`
- `workorder/create`, `workorder/detail`, `workorder/list`

## API contract rules
- API 契约以 `04-openapi/app-portal.yaml` 与 `04-openapi/payment.yaml` 的 `/api/app/v1` 端点为准；若契约尚未就绪，先按契约占位并标记 `SPEC-GAP`，不得自行臆造字段。
- 请求层统一 `/api/app/v1` 前缀；错误码映射统一走 `10-registries/error-codes.yaml`。

## Login state and membership switching
- 登录态、token 刷新与 membership（成员身份）切换遵循 `docs/architecture/USER-APP-STRUCTURE.md`。

## Amount display rule
- 金额一律以 string 类型接收与展示，禁止浮点数运算（不使用 Number/float 参与金额计算或格式化）。

## Agent preflight output
Before generating code, state:
1. pages to modify (from the 25-page checklist);
2. API endpoints consumed (traced to `04-openapi/app-portal.yaml` and `04-openapi/payment.yaml` `/api/app/v1`);
3. login/token-refresh and membership switching logic involved;
4. amount display and error-code mapping strategy;
5. any `SPEC-GAP` found.

## DoD
- all 25 pages runnable;
- request layer uses unified `/api/app/v1` prefix;
- token refresh works;
- error codes mapped via `10-registries/error-codes.yaml`;
- amounts rendered as strings (no floating-point).

## Completion rule
Generated code is not complete until the task DoD and V7 release gates relevant to this module pass.
