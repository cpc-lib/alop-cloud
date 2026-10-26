# TASK-002 Tenant Create Request Identity Design

**Date:** 2026-10-25  
**Status:** Proposed design for the next implementation slice  
**Decision:** Use a versioned CreateTenant contract with an explicit request identity.

## Context

The Tenant domain requires `CreateTenant requestId`, while the idempotency matrix defines the guard as `Idempotency-Key + tenant_create_request_id`. The current V1 OpenAPI request has no `tenantCreateRequestId`, and the frozen Tenant V1 table has no durable request identity column. Making the field required in the existing V1 endpoint would be a breaking REST change.

The V7 freeze policy requires breaking REST changes to use a new version and requires DDL changes to follow Expand -> Migrate -> Contract. The existing `idempotency_record` remains the retry/idempotency-key guard; the Tenant row will carry the independent business request identity.

## Decision

### Public contract

- Keep the existing `/api/platform/v1/tenants` contract unchanged.
- Add a versioned CreateTenant contract at `/api/platform/v2/tenants`.
- V2 requires `tenantCreateRequestId` in the request body and continues to require the `Idempotency-Key` header.
- The V2 request identity is a stable client-generated string, 8-128 characters, and is not generated afresh for retries.

### Persistence

- Keep `spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql` unchanged.
- Add a versioned Tenant migration that adds `tenant_create_request_id VARCHAR(128) NOT NULL` and a unique key on that column after deterministic backfill of existing rows.
- The application always supplies a non-null request identity for new Tenant rows; the database unique key is the final cross-request guard.
- Use the existing integration idempotency table with platform sentinel scope `tenant_id=0`, `user_id=0`, and `api_code='TENANT_CREATE_V2'`. This avoids relying on MySQL NULL uniqueness for the header key and does not change the unapproved integration-schema ADR.

### Local transaction

The CreateTenant application service owns one JDBC transaction:

1. Insert a pending idempotency record for the platform request key, or lock the existing record.
2. For an existing record, compare the stored request hash. Same hash replays the stored result; a different hash returns `IDEMPOTENCY_KEY_CONFLICT`.
3. Insert the Tenant aggregate in `CREATING` with the explicit `tenant_create_request_id`.
4. Complete the idempotency record with the created Tenant result.
5. Commit both facts together.

No external call, Outbox publish, provisioning step, or RabbitMQ operation runs inside this transaction. A failed transaction rolls back both the Tenant row and the idempotency record, allowing a safe retry.

### Conflict semantics

- Same `Idempotency-Key` and same request hash: return the original Tenant result.
- Same `Idempotency-Key` and different request hash: `IDEMPOTENCY_KEY_CONFLICT`.
- Different `Idempotency-Key` and same `tenantCreateRequestId`: stable Tenant create request conflict.
- Duplicate `tenantCode`: stable Tenant create request conflict until a dedicated tenant-code error is registered.

The application layer will not silently treat a different request identity as a replay.

## Implementation boundary

This slice includes:

- V2 OpenAPI contract and catalog entry for CreateTenant.
- Versioned Tenant migration and disposable MySQL loading.
- Tenant create request identity on the domain rehydration/persistence boundary.
- Idempotency repository port and JDBC adapter for the existing integration table.
- CreateTenant command and application service with explicit JDBC transaction orchestration.
- Unit, MySQL integration, duplicate, concurrent, and rollback tests.

This slice does not include REST controller wiring, permission provider integration, provisioning Saga, `TenantCreated` Outbox event, quota/config/feature/route initialization, or SupportSession.

## Open SPEC-GAP records

- `TRANSACTION-LOCK-MATRIX.yaml` has no `CreateTenant` operation or lock-order row. This slice uses the idempotency record as the first local guard and then inserts the Tenant row; the authoritative matrix entry still needs review.
- `event-registry.yaml` has no `TenantCreated` event despite the Tenant Domain Spec listing it. This slice does not invent an event schema or routing key.
- The existing nullable `idempotency_record.user_id` uniqueness concern remains covered by the separate proposed ADR; this design uses the platform sentinel `user_id=0` only for the new V2 command.

## Success criteria

- V1 OpenAPI and Tenant V1 DDL remain byte-for-byte unchanged.
- V2 requests require an explicit request identity and can be replayed safely with the same Idempotency-Key.
- Different requests cannot create two Tenant rows with the same request identity.
- Concurrent identical submissions result in one Tenant row and one durable idempotency result.
- Failed create rolls back both the Tenant and idempotency records.
- Real MySQL 8 integration tests and existing regression tests pass.

