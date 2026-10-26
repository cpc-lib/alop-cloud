# TASK-002 Tenant Create Request Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit Tenant create request identity and transactionally idempotent CreateTenant application command without modifying the frozen V1 REST contract or Tenant V1 migration.

**Architecture:** Keep V1 OpenAPI and V1 DDL unchanged. Add a V2 CreateTenant contract, a versioned Tenant migration for `tenant_create_request_id`, and a caller-owned JDBC idempotency adapter over the existing integration table. The application service owns one local JDBC transaction that inserts the idempotency guard, creates the Tenant aggregate, stores the replay result, and commits both facts together.

**Tech Stack:** Java 21, JDBC, MySQL 8, JUnit 5, Flyway-style SQL files, existing Docker-CLI integration runner.

---

### Task 1: Freeze the V2 contract and versioned schema change

**Files:**
- Create: `spec/alop-v7.0/04-openapi/tenant-v2.yaml`
- Modify: `spec/alop-v7.0/11-codegen/API-CATALOG.yaml`
- Modify: `spec/alop-v7.0/11-codegen/TRACEABILITY-MATRIX.csv`
- Create: `spec/alop-v7.0/03-database/flyway/tenant/V2__tenant_create_request_id.sql`
- Modify: `spec/alop-v7.0/10-registries/error-codes.yaml`
- Modify: `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/error/CommonErrors.java`

- [ ] **Step 1: Add the V2 CreateTenant OpenAPI contract.**

Keep the existing V1 file untouched. Define `POST /api/platform/v2/tenants`, retain the required `Idempotency-Key` header, and require `tenantCreateRequestId` with length 8-128 in the request body. Copy the existing CreateTenant fields without changing their meanings and use a distinct operation ID `postTenantsV2`.

- [ ] **Step 2: Register the V2 operation and traceability row.**

Add the V2 operation to `API-CATALOG.yaml` and add a narrow TASK-002 traceability row for the explicit request identity and duplicate-request integration test. Do not rewrite existing catalog rows.

- [ ] **Step 3: Add the versioned Tenant migration.**

Create `V2__tenant_create_request_id.sql` that adds `tenant_create_request_id VARCHAR(128) NULL`, backfills existing rows deterministically as `legacy-tenant-<id>`, changes the column to `NOT NULL`, and adds `uk_tenant_create_request_id`. The migration must not edit `V1__init.sql`.

- [ ] **Step 4: Register the stable request conflict error.**

Add `TENANT_CREATE_REQUEST_CONFLICT` as HTTP 409, non-retryable, with message `租户创建请求已存在`, and mirror it in `CommonErrors`.

- [ ] **Step 5: Parse the new YAML and inspect the frozen diff.**

Run:

```powershell
python -c "import yaml, pathlib; [yaml.safe_load(pathlib.Path(p).read_text(encoding='utf-8')) for p in ['spec/alop-v7.0/04-openapi/tenant-v2.yaml','spec/alop-v7.0/10-registries/error-codes.yaml']]"
git diff -- spec/alop-v7.0/04-openapi/tenant.yaml spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql
```

Expected: YAML parses and the two V1 frozen files have no diff.

### Task 2: Write failing application and MySQL tests

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/pom.xml` for the MySQL test dependency needed by module-local tests.
- Modify: `backend/alop-framework/alop-test-support/pom.xml` for the Tenant create integration test dependency.
- Modify: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/domain/tenant/TenantLifecycleTest.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/application/service/CreateTenantServiceTest.java`
- Create: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationTenantCreateIT.java`

- [ ] **Step 1: Update existing aggregate tests to use an explicit request identity.**

Change only the Tenant factory calls so every new aggregate has a stable request ID; retain all existing lifecycle assertions.

- [ ] **Step 2: Write the application RED tests.**

Cover command validation for missing/short request identity and the service contract for same-key replay, same-key/different-payload conflict, same-request-ID/different-key conflict, and rollback when the Tenant insert fails. Use real repository implementations and a test-owned JDBC connection fixture rather than mocking the business behavior.

- [ ] **Step 3: Write the real MySQL RED integration tests.**

Against `alop_schema_it`, cover one successful create, same key/same hash replay returning the same Tenant ID, same key/different hash returning `IDEMPOTENCY_KEY_CONFLICT`, different key/same `tenantCreateRequestId` returning `TENANT_CREATE_REQUEST_CONFLICT`, two concurrent identical submissions producing one Tenant row, and a failed insert leaving no idempotency record. Refuse any database other than the disposable schema.

- [ ] **Step 4: Run the focused RED command.**

```powershell
cd backend
mvn -pl alop-tenant/alop-tenant-server -am '-Dtest=CreateTenantServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

Expected: compilation/test failure because the request identity, idempotency port, adapter, command and service are not implemented yet.

### Task 3: Extend the Tenant aggregate and repositories

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/tenant/Tenant.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/repository/TenantRepository.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/persistence/repository/JdbcTenantRepository.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/repository/TenantCreateIdempotencyRepository.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/persistence/repository/JdbcTenantCreateIdempotencyRepository.java`

- [ ] **Step 1: Add validated request identity to Tenant.**

Require an 8-128 character `tenantCreateRequestId` in `creating` and `rehydrate`, expose it read-only, and preserve domain-owned state transitions. No public status setter is added.

- [ ] **Step 2: Extend Tenant persistence SQL.**

Read and write `tenant_create_request_id` in the Tenant repository while preserving caller-owned transaction semantics, `FOR UPDATE`, optimistic version checks and the existing lifecycle SQL behavior.

- [ ] **Step 3: Define the idempotency repository port.**

Expose only the operations needed by CreateTenant: insert a pending platform record, lock/read an existing record by the exact scope and key, and complete the record with response code/body. All methods receive the caller-owned `Connection` and never commit.

- [ ] **Step 4: Implement the JDBC idempotency adapter.**

Use `tenant_id=0`, `user_id=0`, `api_code='TENANT_CREATE_V2'`, and a long-lived expiry. Treat only the unique-key duplicate as the existing-key branch; preserve other SQL failures. Store the request hash and replay response in the same transaction as the Tenant insert.

- [ ] **Step 5: Run the module tests.**

```powershell
mvn -pl alop-tenant/alop-tenant-server -am test -B
```

Expected: aggregate and application tests pass; no integration test is claimed until the Docker runner is green.

### Task 4: Implement the CreateTenant command and transaction orchestration

**Files:**
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/command/CreateTenantCommand.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/service/CreateTenantService.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/service/CreateTenantResult.java`

- [ ] **Step 1: Define the command/result boundary.**

The command contains the two identities, Tenant core fields and optional package ID; the result contains the created Tenant ID, request identity, status and version. Validate all required strings before opening a connection.

- [ ] **Step 2: Implement deterministic request hashing.**

Hash the canonical command fields with SHA-256 in a fixed field order. Do not use object serialization or a nondeterministic map. A changed Tenant field under the same Idempotency-Key must produce `IDEMPOTENCY_KEY_CONFLICT`.

- [ ] **Step 3: Implement the local transaction.**

Open a connection from the injected `DataSource`, disable auto-commit, insert or lock the idempotency record, replay an identical completed request, insert `Tenant.creating`, complete the idempotency record, commit, and roll back on every failure. Map unique request identity or Tenant code conflicts to `TENANT_CREATE_REQUEST_CONFLICT`.

- [ ] **Step 4: Run the unit tests and focused service tests.**

```powershell
mvn -pl alop-tenant/alop-tenant-server -am test -B
```

Expected: all Tenant lifecycle and CreateTenant service tests pass.

### Task 5: Run the disposable integration suite and record evidence

**Files:**
- Modify: `scripts/verify_rabbit_mysql_docker.py` to load Tenant V2 after V1 and include `IntegrationTenantCreateIT`.
- Create: `docs/development/implementation/TASK-002-TENANT-CREATE-IDEMPOTENCY.md`
- Modify: `docs/project-management/PROGRESS.md` with exact test results and remaining gaps.

- [ ] **Step 1: Run the real MySQL/RabbitMQ suite.**

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: the existing 34 tests plus the new CreateTenant integration scenarios pass and all disposable containers are cleaned up.

- [ ] **Step 2: Run final verification.**

```powershell
python scripts/validate_project.py
git diff --check
git diff -- spec/alop-v7.0/04-openapi/tenant.yaml spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql
```

Expected: project validation and whitespace checks pass, and frozen V1 files remain unchanged.

- [ ] **Step 3: Record non-goals.**

Explicitly leave REST controller wiring, permission provider integration, Tenant provisioning Saga, `TenantCreated` Outbox event, quota/config/feature/route initialization, SupportSession and the missing transaction/event registry entries open.

