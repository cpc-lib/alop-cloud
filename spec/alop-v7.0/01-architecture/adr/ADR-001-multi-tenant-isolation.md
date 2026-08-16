# ADR-001 Multi-Tenant Isolation

**Decision:** 默认 Shared DB + Shared Schema，业务表 tenant_id NOT NULL；Repository/SQL Fail Closed。

**Rules:** TenantContext 来自已认证 membership；业务 API 不信任 body/header tenantId；Redis/ES/MQ/MinIO 均 namespaced；Platform Support 使用有时效 SupportSession；高价值租户可迁 Dedicated DB。

**Rejected:** 仅前端菜单隔离、仅 Gateway 注入 tenantId、允许 Repository 在 tenant 缺失时查询全表。
