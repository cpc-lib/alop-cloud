# Tenant Isolation Security SPEC

## Fail Closed
- No TenantContext => business repository/query/write must fail with `TENANT_CONTEXT_REQUIRED`.
- Client tenant header is only a requested tenant selector and must match authenticated membership.
- All business unique indexes include tenant_id unless the identifier is deliberately globally unique (payment_no etc.).

## Layers
1. Gateway validates token and selected membership.
2. Service builds immutable TenantContext.
3. MyBatis tenant interceptor injects tenant predicate for whitelisted tenant tables.
4. Repository methods accept TenantId explicitly for high-risk modules.
5. Redis keys: `alop:{tenantId}:...`.
6. ES shared index queries always filter tenantId; dedicated index optional.
7. MQ EventEnvelope always contains tenantId; consumer revalidates business aggregate tenant.
8. MinIO path starts with tenantId; file URL generation re-authorizes tenant + business ACL.

## Platform Support
No implicit super-admin bypass. SupportSession must have tenantId, reason, approver, start/end, scoped permissions and audit every request.
