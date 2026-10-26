# TASK-002 Tenant Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Persist the Tenant lifecycle aggregate against the frozen Tenant V1 schema with explicit transactions, `FOR UPDATE` reads, and optimistic version protection.

**Architecture:** Add a small domain repository port and a JDBC adapter owned by `alop-tenant`. The adapter reads only the Tenant aggregate columns, inserts using frozen-table defaults, locks lifecycle reads with `SELECT ... FOR UPDATE`, and updates `status` only when the expected version matches. Real MySQL verification runs in the existing Docker-CLI integration harness; no REST, Flyway copy, Outbox event, or application command is added in this slice.

**Tech Stack:** Java 21, JDBC, MySQL 8, JUnit 5, existing Docker-CLI Maven integration runner.

---

### Task 1: Add failing real-MySQL repository tests

**Files:**
- Modify: `backend/alop-framework/alop-test-support/pom.xml` to depend on `alop-tenant-server` for tests.
- Create: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationTenantLifecycleIT.java`

- [x] **Step 1: Write tests for insert, locked reload, lifecycle update, and stale version rejection.**

The test must use `alop.mysql.jdbcUrl` and `alop.mysql.password`, refuse non-`alop_schema_it` databases, and exercise the repository with real `Connection` objects. Cover:

```java
repository.insert(connection, Tenant.creating(300L, "tenant-300", "Tenant 300", "ENTERPRISE", null));
Tenant loaded = repository.findByIdForUpdate(connection, 300L).orElseThrow();
loaded.provisionSucceeded();
repository.updateLifecycle(connection, loaded, 0L);
assertEquals(TenantStatus.ACTIVE, repository.findById(connection, 300L).orElseThrow().status());
```

Also load two stale copies and assert the second lifecycle update throws `CommonErrors.OPTIMISTIC_LOCK_CONFLICT`; verify repository methods reject auto-commit connections for lifecycle mutation/locking and return empty for an unknown ID.

- [x] **Step 2: Run the focused test to confirm RED.**

```powershell
cd backend
mvn -pl alop-framework/alop-test-support -am '-Dtest=IntegrationTenantLifecycleIT' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dalop.mysql.jdbcUrl=jdbc:mysql://127.0.0.1:1/alop_schema_it' '-Dalop.mysql.password=invalid' test -B
```

Expected: compilation failure because the repository port/adapter and test-support dependency do not yet exist; no network or production database may be used.

### Task 2: Implement the Tenant repository port and JDBC adapter

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/tenant/Tenant.java` with a validated rehydration factory.
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/repository/TenantRepository.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/persistence/repository/JdbcTenantRepository.java`

- [x] **Step 1: Define the port.**

Use these methods and checked failure semantics:

```java
Optional<Tenant> findById(Connection connection, long tenantId) throws SQLException;
Optional<Tenant> findByIdForUpdate(Connection connection, long tenantId) throws SQLException;
void insert(Connection connection, Tenant tenant) throws SQLException;
void updateLifecycle(Connection connection, Tenant tenant, long expectedVersion) throws SQLException;
```

- [x] **Step 2: Implement frozen-schema SQL.**

Use `tenant(id,tenant_code,tenant_name,tenant_type,status,package_id,version)` only. `findByIdForUpdate` must require `autoCommit=false` and append `FOR UPDATE`; `insert` and `updateLifecycle` must require an explicit transaction. `updateLifecycle` must execute:

```sql
UPDATE tenant
   SET status=?, version=version+1, updated_at=UTC_TIMESTAMP(3)
 WHERE id=? AND version=?
```

Map zero updated rows to `BusinessException(CommonErrors.OPTIMISTIC_LOCK_CONFLICT)`. Never accept a caller-supplied status string outside `TenantStatus` and never update `tenant_id` or unrelated tables.

- [x] **Step 3: Run the focused integration test with the disposable Docker harness.**

The test must pass only against the isolated `alop_schema_it` database populated from the frozen Tenant V1 DDL. A missing external database is an environment failure, not a code pass.

### Task 3: Extend the Docker-CLI regression runner

**Files:**
- Modify: `scripts/verify_rabbit_mysql_docker.py` to load `spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql` and include `IntegrationTenantLifecycleIT` in the bounded test selector.

- [x] **Step 1: Run the full bounded MySQL/Rabbit regression.**

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: the existing 31 tests plus the Tenant repository tests pass, and all disposable containers are removed.

### Task 4: Record evidence and check frozen boundaries

**Files:**
- Create: `docs/development/implementation/TASK-002-TENANT-PERSISTENCE.md`
- Modify: `docs/project-management/PROGRESS.md` by appending a dated evidence section.

- [x] **Step 1: Record exact SQL/transaction behavior and remaining gaps.**

State that this slice implements repository persistence only; application command transactions, CreateTenant idempotency, Outbox events, REST, quota/config/feature/route, SupportSession, permissions, audit/metrics and full Tenant A/B isolation remain open.

- [x] **Step 2: Verify the frozen boundaries.**

```powershell
python scripts/validate_project.py
git diff --check
git diff -- spec/alop-v7.0/04-openapi/tenant.yaml spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql
```

Expected: POM/YAML validation and whitespace checks pass; frozen OpenAPI and Tenant V1 DDL remain unchanged.
