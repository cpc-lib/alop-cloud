# ADR-009 Tenant Routing

TenantRoute 维护 SHARED_SCHEMA / SEPARATE_SCHEMA / DEDICATED_DB。路由属于 P0 元数据，修改需审计和高风险权限；迁移保持业务 ID 不变。
