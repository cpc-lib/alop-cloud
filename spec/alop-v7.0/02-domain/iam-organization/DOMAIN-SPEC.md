# IAM-ORGANIZATION DOMAIN SPEC

## 1. Bounded Context / Service
`alop-iam + alop-organization`

## 2. Aggregate Roots
- `UserMembership`
- `Role`
- `OrganizationUnit`
- `ManagementTeam`

## 3. Owned Tables
- `platform_user`
- `tenant_user_membership`
- `role`
- `permission`
- `role_permission`
- `user_role`
- `organization_unit`
- `management_team`
- `management_team_member`
- `team_resource_relation`
- `resource_acl`
- `ownership_history`

## 4. Commands
- `AddMembership`
- `DisableMembership`
- `AssignRole`
- `CreateOrganization`
- `CreateTeam`
- `TransferOwnership`

## 5. Queries
- `GetCurrentPermissions`
- `GetDataScope`
- `ListOrganizationTree`

## 6. Produced Events
- `MembershipDisabled`
- `PermissionChanged`
- `CustomerOwnerChanged`
- `ResourceOwnerChanged`

## 7. Permissions
- `tenant:user:manage`
- `tenant:role:manage`
- `organization:team:manage`

## 8. Invariants
- `role belongs to tenant`
- `tenant membership required before role assignment`
- `resource ACL never grants cross-tenant access`
- `platform support access requires SupportSession`

## 9. Transaction / Locking
- `optimistic version for role/team updates`

## 10. Idempotency
- `membership invitation token`

## 11. Closure Condition
Disabled member must have pending ownership/workflow tasks transferred or explicit transfer task created.

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
