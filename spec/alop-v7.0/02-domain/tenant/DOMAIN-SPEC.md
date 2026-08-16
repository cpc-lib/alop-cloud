# TENANT DOMAIN SPEC

## 1. Bounded Context / Service
`alop-tenant`

## 2. Aggregate Roots
- `Tenant`
- `TenantPackage`
- `TenantRoute`

## 3. Owned Tables
- `tenant`
- `tenant_package`
- `tenant_quota`
- `tenant_config`
- `tenant_feature`
- `tenant_route`
- `tenant_branding`
- `support_session`

## 4. Commands
- `CreateTenant`
- `SuspendTenant`
- `ResumeTenant`
- `TerminateTenant`
- `UpdateTenantRoute`
- `ChangeTenantPackage`

## 5. Queries
- `GetTenant`
- `ListTenants`
- `GetTenantUsage`

## 6. Produced Events
- `TenantCreated`
- `TenantSuspended`
- `TenantResumed`
- `TenantTerminated`
- `TenantRouteChanged`

## 7. Permissions
- `platform:tenant:create`
- `platform:tenant:suspend`
- `platform:tenant:route:update`

## 8. Invariants
- `tenantCode unique globally`
- `business tenant APIs require ACTIVE except explicitly allowed callback/settlement paths`
- `route changes are audited and fail closed`
- `package capability AND tenant feature determines feature availability`

## 9. Transaction / Locking
- `tenant row FOR UPDATE for lifecycle transitions`

## 10. Idempotency
- `CreateTenant requestId`

## 11. Closure Condition
Tenant TERMINATED only after in-flight payment/refund/invoice/reconciliation and export/retention checks.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.
