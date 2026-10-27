# Tenant Create Prerequisites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with review checkpoints. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Tenant create prerequisites and a fail-closed v2 REST boundary without claiming a successful admin binding before IAM provisioning exists.

**Architecture:** Resolve `packageCode` to an ACTIVE package ID inside the existing JDBC transaction. Keep platform actor resolution and `platform:tenant:create` authorization behind application ports with fail-closed production adapters. Wire repositories, Snowflake ID suppliers, and the create service through Spring, while the v2 endpoint returns a stable not-ready error until admin provisioning has a durable owner.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring JDBC, MySQL 8, JUnit 5, Maven, existing `SnowflakeIdGenerator` and JDBC transaction style.

---

## File map

- Create `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/repository/TenantPackageRepository.java` for the package lookup port.
- Create `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/persistence/repository/JdbcTenantPackageRepository.java` for the caller-owned JDBC adapter.
- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/command/CreateTenantCommand.java` to carry `packageCode` and include it in the request hash.
- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/service/CreateTenantService.java` to resolve the ACTIVE package in the same transaction.
- Create `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/PlatformActor.java`, `PlatformActorResolver.java`, and `TenantCreateAuthorizer.java` for trusted identity boundaries.
- Create fail-closed security adapters under `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/`.
- Create `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/config/TenantCreateConfiguration.java` for Spring beans and required worker-id configuration.
- Create the v2 request DTO and fail-closed controller under `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/`.
- Modify `spec/alop-v7.0/10-registries/error-codes.yaml` and `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/error/CommonErrors.java` for `TENANT_PACKAGE_NOT_FOUND` and `TENANT_CREATE_NOT_READY`.
- Modify `backend/alop-tenant/alop-tenant-server/pom.xml` and `application.yml` only for JDBC/runtime configuration required by these beans.
- Extend existing Tenant unit and MySQL integration tests; add focused security/config/controller tests beside the implementation.

### Task 1: Add stable errors and the package lookup port

**Files:**
- Modify: `spec/alop-v7.0/10-registries/error-codes.yaml`
- Modify: `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/error/CommonErrors.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/repository/TenantPackageRepository.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/persistence/repository/JdbcTenantPackageRepository.java`
- Test: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationTenantCreateIT.java`

- [x] **Step 1: Write the failing integration assertions.**

Update `IntegrationTenantCreateIT` to construct a package repository, insert `packageCode=standard` with `status=ACTIVE` in `clean()`, and call the create command with `standard`. Add one test that inserts only an INACTIVE package and expects `TENANT_PACKAGE_NOT_FOUND` from the service. The test must use the real MySQL connection and assert no tenant row is created.

- [x] **Step 2: Run the focused test and verify the expected RED failure.**

Run:

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: compilation/test failure because `TenantPackageRepository` and the package-code command path do not exist yet. Do not treat an unavailable Docker engine as a feature failure; if it is unavailable, run the Tenant unit test in Task 2 after the source-level test is written and record the integration retry for Task 6.

- [x] **Step 3: Add the stable error definitions.**

Add these exact definitions:

```java
public static final ErrorDefinition TENANT_PACKAGE_NOT_FOUND =
        new ErrorDefinition("TENANT_PACKAGE_NOT_FOUND", 404, false, "有效租户套餐不存在");
public static final ErrorDefinition TENANT_CREATE_NOT_READY =
        new ErrorDefinition("TENANT_CREATE_NOT_READY", 503, true, "租户管理员绑定能力尚未就绪");
```

Register the same codes under the `tenant` section of `error-codes.yaml`; keep V1 OpenAPI and V1 DDL unchanged.

- [x] **Step 4: Implement the minimal repository port and JDBC adapter.**

Use this port shape:

```java
public interface TenantPackageRepository {
    OptionalLong findActiveIdByCode(Connection connection, String packageCode) throws SQLException;
}
```

The JDBC adapter must execute a parameterized query selecting `id` from `tenant_package` where `package_code=? AND status='ACTIVE'`, require an explicit transaction, and return `OptionalLong.empty()` for no row. It must not create or mutate package rows.

- [x] **Step 5: Re-run the focused integration test and verify GREEN.**

Run the same Docker runner. Expected: the new package lookup test and all previously covered Tenant create tests pass; no V1 schema file changes are present.

### Task 2: Switch CreateTenant to packageCode and preserve transaction semantics

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/command/CreateTenantCommand.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/service/CreateTenantService.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/application/service/CreateTenantServiceTest.java`
- Modify: all current `new CreateTenantCommand(...)` call sites found by `rg`

- [x] **Step 1: Write the failing unit tests.**

Add tests for these behaviors using a stub `TenantPackageRepository` and the existing test data source seam. The package-code hash test is:

```java
@Test
void packageCodeParticipatesInTheCanonicalRequestHash() {
    CreateTenantCommand standard = new CreateTenantCommand(
            "idem-key-1", "request-1", "tenant-1", "Tenant 1", "ENTERPRISE", "standard");
    CreateTenantCommand premium = new CreateTenantCommand(
            "idem-key-1", "request-1", "tenant-1", "Tenant 1", "ENTERPRISE", "premium");

    assertNotEquals(standard.canonicalForm(), premium.canonicalForm());
}

@Test
void inactiveOrMissingPackageIsRejectedBeforeTenantInsert() throws Exception {
    BusinessException failure = assertThrows(BusinessException.class,
            () -> serviceWithMissingPackage().create(validCommand("missing-package")));

    assertEquals(CommonErrors.TENANT_PACKAGE_NOT_FOUND, failure.error());
    assertEquals(0, countTenants());
}
```

`serviceWithMissingPackage()` is a test helper that constructs the real service with the existing test DataSource, a stub package repository returning `OptionalLong.empty()`, the existing JDBC tenant/idempotency repositories, and monotonic test ID suppliers. `validCommand(String packageCode)` returns a command with valid fixed values and the supplied package code. Keep the test focused on behavior, not private implementation details.

- [x] **Step 2: Run the unit test and verify RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=CreateTenantServiceTest test -B
```

Expected: compilation or assertion failure because the command still accepts `packageId` and the service does not resolve package codes.

- [x] **Step 3: Implement the minimal command/service change.**

Change the command component from `Long packageId` to `String packageCode`; require non-blank code with a maximum of 64 characters and include it in `canonicalForm()`. Inject `TenantPackageRepository` into `CreateTenantService`. After the idempotency pending record is inserted and before `Tenant.creating`, call `findActiveIdByCode(connection, command.packageCode())`; throw `TENANT_PACKAGE_NOT_FOUND` when empty; pass the resolved ID to the aggregate. Keep replay before package lookup, keep the same commit/rollback blocks, and preserve existing duplicate and idempotency errors.

- [x] **Step 4: Run the unit tests and verify GREEN.**

Run the focused command and service test, then:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test -B
```

Expected: the focused tests and the complete Tenant/common reactor pass.

### Task 3: Add trusted actor and fail-closed authorization boundaries

**Files:**
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/PlatformActor.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/PlatformActorResolver.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/TenantCreateAuthorizer.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/FailClosedPlatformActorResolver.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/FailClosedTenantCreateAuthorizer.java`
- Test: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/application/security/PlatformActorTest.java`
- Test: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/infrastructure/security/FailClosedTenantCreateSecurityTest.java`

- [x] **Step 1: Write failing tests.**

Test that `PlatformActor` rejects zero or negative IDs, that the fail-closed resolver throws `BusinessException` with `TENANT_ACCESS_DENIED`, and that the fail-closed authorizer rejects even a valid actor. Do not use `adminUserId` in any of these tests.

- [x] **Step 2: Run the tests and verify RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest="*Actor*Test,*Authorizer*Test" test -B
```

Expected: missing-type or assertion failures.

- [x] **Step 3: Implement the minimal ports and adapters.**

Use `PlatformActor(long userId)` with `userId > 0`, `PlatformActorResolver.resolve()` returning a trusted actor, and `TenantCreateAuthorizer.requireCreate(PlatformActor actor)` returning normally only when authorized. The default adapters always throw `new BusinessException(CommonErrors.TENANT_ACCESS_DENIED)`.

- [x] **Step 4: Run the focused and complete Tenant tests.**

Expected: all new security tests and all existing Tenant/common tests pass.

### Task 4: Wire production JDBC, ID generation, and fail-closed beans

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/pom.xml`
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/resources/application.yml`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/config/TenantCreateConfiguration.java`
- Test: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/infrastructure/config/TenantCreateConfigurationTest.java`

- [x] **Step 1: Write the failing configuration tests.**

Add a Spring context test with `alop.tenant.worker-id=7` and a test `DataSource` that asserts the context contains `SnowflakeIdGenerator`, both `LongSupplier` beans, `JdbcTenantRepository`, `JdbcTenantPackageRepository`, `JdbcTenantCreateIdempotencyRepository`, `CreateTenantService`, and the fail-closed actor/authorizer beans. Add a second context test with no worker-id property and assert startup fails with an unresolved required property.

- [x] **Step 2: Run the configuration tests and verify RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=TenantCreateConfigurationTest test -B
```

Expected: missing configuration class/beans or missing JDBC dependency.

- [x] **Step 3: Implement the minimal Spring configuration.**

Add `spring-boot-starter-jdbc`, `mysql-connector-j` runtime, and `spring-boot-test` test dependency only where required. Define `TenantCreateConfiguration` with a required constructor property `@Value("${alop.tenant.worker-id}") long workerId`; expose one `SnowflakeIdGenerator`, two suppliers backed by `nextId()`, the three JDBC repositories, the create service, and fail-closed security adapters. Add:

```yaml
alop:
  tenant:
    worker-id: ${ALOP_TENANT_WORKER_ID}
```

Do not add a fallback worker ID or hard-coded datasource credentials.

- [x] **Step 4: Run configuration and complete module tests.**

Expected: configured context passes, missing worker ID fails, and no existing unit test regresses.

### Task 5: Add the v2 request DTO and a not-ready REST boundary

**Files:**
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/CreateTenantRequestV2.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/TenantCreateV2Controller.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/TenantCreateError.java`
- Test: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/interfaces/rest/TenantCreateV2ControllerTest.java`

- [x] **Step 1: Write failing controller tests.**

Test that a request containing all V2 fields and a valid `Idempotency-Key` reaches the actor boundary and returns `403 TENANT_ACCESS_DENIED` with the default fail-closed resolver. Test that a valid actor cannot produce `200` while the admin binding readiness is unavailable; it must return `503 TENANT_CREATE_NOT_READY`. Test malformed/missing idempotency key and missing required body values return `400` without invoking any repository or service.

- [x] **Step 2: Run the controller tests and verify RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=TenantCreateV2ControllerTest test -B
```

Expected: missing DTO/controller/error mapping or incorrect status codes.

- [x] **Step 3: Implement the thin fail-closed adapter.**

Create a record matching the V2 fields: `tenantCode`, `tenantName`, `tenantType`, `packageCode`, `adminUserId`, `tenantCreateRequestId`, `timezone`, and `currency`. Validate required text, positive `adminUserId`, request ID length, and header length before calling application boundaries. Map `BusinessException` to its stable HTTP status/code. The controller must not call `CreateTenantService` in this slice; after successful actor/permission checks it returns `TENANT_CREATE_NOT_READY` so no `adminUserId` is silently discarded. Keep the endpoint path and method exactly `/api/platform/v2/tenants` and `POST`.

- [x] **Step 4: Run controller and full Tenant tests.**

Expected: 400/403/503 mappings pass and no 200 response is emitted by this slice.

### Task 6: Run regression and update evidence

**Files:**
- Modify: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationTenantCreateIT.java`
- Modify: `scripts/verify_rabbit_mysql_docker.py` only if the new package seed needs a runner hook
- Modify: `docs/development/implementation/TASK-002-TENANT-CREATE-IDEMPOTENCY.md` and `docs/project-management/PROGRESS.md`

- [x] **Step 1: Verify source and contract boundaries.**

Run:

```powershell
python scripts/validate_project.py
python -m py_compile scripts/verify_rabbit_mysql_docker.py
git diff --check
```

Parse the changed YAML files and assert the frozen V1 OpenAPI and V1 Tenant DDL bytes are unchanged.

- [x] **Step 2: Run the complete local Maven verification.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test -B
mvn -f backend/pom.xml -pl alop-framework/alop-test-support -am -DskipTests test-compile -B
```

Expected: BUILD SUCCESS, all Tenant/common tests green.

- [x] **Step 3: Run the real Docker-backed regression.**

Run:

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: prior 39/39 scenarios plus the package-code and not-ready coverage pass, with zero failures/errors/skips; verify no `alop-mq-*` containers remain afterward.

- [x] **Step 4: Record evidence and inspect the final diff.**

Update the implementation evidence with exact test counts and the explicit limitation that v2 success remains blocked by IAM admin provisioning. Review `git diff --stat` and `git status --short`; do not stage or commit unrelated pre-existing worktree changes.

