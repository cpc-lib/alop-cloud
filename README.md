# ALOP Cloud v1.1 — V7.0 SPEC Integrated

企业级多租户房源资产经营、招商 CRM、出租/销售、合同履约、计费、支付、
收款核销、发票、账务、对账、审批、售后与运营平台。

本仓库已经整合：

- **完整 ALOP-SaaS V7.0 Frozen Codegen SPEC**
- yudao-cloud 风格 Maven `api + server` 技术骨架
- DDD 四层业务服务骨架
- 21 个业务/平台服务
- 18 个 framework starter 模块
- React 管理端 Skeleton
- UniApp 用户端 Skeleton
- Docker / deployment 基础目录
- 单人 + AI 的 **80 周 / 40 Sprint / 约 20 个月**全量实施计划

## 业务与技术的关系

```text
yudao-cloud
    ↓
Technical Foundation Reference

ALOP V7.0 SPEC
    ↓
Business Source of Truth
    ↓
api + server
    ↓
DDD Application / Domain
    ↓
Infrastructure Adapters
```

## 先看这里

### 业务 SPEC
- `SPEC-ENTRYPOINT.md`
- `spec/alop-v7.0/START-HERE.md`
- `spec/alop-v7.0/00-master/MASTER-SPEC-V7.0.md`

### 技术架构
- `docs/architecture/ALOP-V7.0-YUDAO-CLOUD-ADAPTATION.md`
- `docs/architecture/TARGET-ARCHITECTURE.md`
- `docs/architecture/YUDAO-ADOPTION.md`

### 项目周期
- `docs/project-management/ALOP全量实现_单人AI_20个月计划.md`
- `docs/project-management/MASTER-ROADMAP.md`
- `docs/project-management/MILESTONES.md`
- `docs/project-management/RELEASE-PLAN.md`
- `docs/project-management/PROGRESS.md`

## 当前真实状态

```text
V7.0 Full SPEC              READY
Architecture Skeleton       READY
21 api+server modules       READY
React Admin Skeleton        READY
UniApp User App Structure   READY
Business Implementation     NOT STARTED / SKELETON ONLY
Production Hardening        NOT STARTED
```

目录存在不等于业务已经实现。

## 一致性原则

默认跨服务：

```text
Local MySQL TX
  + Outbox
  + RabbitMQ
  + Inbox
  + Persisted Saga
```

Seata AT 不作为默认正确性方案。

## V7.0 核心红线

- TenantContext 来自认证 Membership，不信任任意客户端 TenantId。
- Redis 不是 Resource Inventory Truth。
- Flowable runtime table 不是业务真相。
- Payment Client SDK success 不是资金真相。
- `Bill != Receivable`。
- Payment Service 不直接修改 Bill/Receivable。
- Finance history 不直接 DELETE/UPDATE 修账。
- Invoice / Refund / Payment / AP Payout UNKNOWN 必须先查询再处理。
- 不允许 MySQL + ES 业务双写。
