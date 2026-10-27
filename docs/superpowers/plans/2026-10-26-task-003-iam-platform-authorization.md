# TASK-003 IAM Platform Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Implement an IAM-owned platform role/permission model and a fail-closed internal authorization API consumed by Tenant v2 without opening tenant creation success.

**Architecture:** Add four IAM-owned platform authorization tables that do not contain `tenant_id` and do not reuse tenant memberships. IAM independently validates a platform JWT and the active `platform_user -> platform_user_role -> platform_role_permission -> platform_permission` chain through a new mTLS internal endpoint. Tenant calls that endpoint through one synchronous authorization port; deny and transport failures remain fail-closed, and an authorized request still stops at the existing `TENANT_CREATE_NOT_READY` boundary.

**Tech Stack:** Java 21, Spring Boot 3.5 MVC/JDBC, Spring Security OAuth2 JOSE, Java `HttpClient` + `RestClient`, MySQL 8, Flyway SQL, JUnit 5, existing Docker-based MySQL/RabbitMQ verification.

---

## Working rules

- The checkout already contains unrelated user changes and prior uncommitted implementation. Do not reset, clean, or stage unrelated files.
- Stage only files named by the current task when a checkpoint is needed. Keep implementation changes uncommitted unless the user explicitly requests an implementation commit; the design and plan documents are already committed separately.
- Preserve these frozen files byte-for-byte: `spec/alop-v7.0/03-database/flyway/iam/V1__init.sql`, `spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql`, `spec/alop-v7.0/04-openapi/tenant.yaml`, and the existing `/internal/iam/v1/membership/verify` contract.
- Use TDD for Java behavior: write the failing test, run the focused test to prove RED, add the smallest implementation, then run the focused test to prove GREEN.

## Task 1: Freeze the IAM platform authorization contract and migration

**Files:**

- Create: `spec/alop-v7.0/03-database/flyway/iam/V2__platform_authorization.sql`
- Modify: `spec/alop-v7.0/02-domain/iam-organization/DOMAIN-SPEC.md`
- Modify: `backend/alop-iam/MODULE-SPEC.md`
- Modify: `spec/alop-v7.0/04-openapi/iam-internal.yaml`
- Modify: `spec/alop-v7.0/11-codegen/API-CATALOG.yaml`
- Modify: `spec/alop-v7.0/11-codegen/TRACEABILITY-MATRIX.csv`

- [ ] **Step 1: Write the exact platform table migration.**

Create `V2__platform_authorization.sql` with four tables. Use no `tenant_id`, no foreign keys, and no default user assignment. The migration must contain the following shape and deterministic catalog seed:

```sql
CREATE TABLE platform_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level INT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_platform_role_code(role_code)
);

CREATE TABLE platform_permission (
    id BIGINT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_platform_permission_code(permission_code)
);

CREATE TABLE platform_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(role_id, permission_id)
);

CREATE TABLE platform_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by BIGINT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(user_id, role_id),
    KEY idx_platform_user_role_status(user_id, status)
);

INSERT INTO platform_permission
    (id, permission_code, permission_name, module_code, status)
VALUES
    (700000000001, 'platform:tenant:create', 'Create tenant', 'platform', 'ACTIVE');

INSERT INTO platform_role
    (id, role_code, role_name, status, risk_level)
VALUES
    (700000000001, 'PLATFORM_TENANT_OPERATOR', 'Platform tenant operator', 'ACTIVE', 3);

INSERT INTO platform_role_permission(role_id, permission_id)
VALUES (700000000001, 700000000001);
```

The reserved seed IDs identify only the catalog rows in the new tables. Do not insert a row into `platform_user_role`; no default administrator is allowed.

- [ ] **Step 2: Update domain ownership and invariants.**

Extend `DOMAIN-SPEC.md` with `PlatformRole` and `PlatformPermission` as IAM-owned platform authorization concepts, list the four tables under owned tables, and state these invariants exactly:

```text
platform authorization never requires tenant_user_membership;
platform user must be ACTIVE;
platform role, permission, role-permission link, and user-role assignment must be ACTIVE/present;
platform scope never uses tenant_id=0 as an identity substitute;
platform support access still requires SupportSession.
```

Update `backend/alop-iam/MODULE-SPEC.md` to state that IAM is the only writer of the new tables and that callers use the internal mTLS authorization endpoint.

- [ ] **Step 3: Add the internal OpenAPI operation and catalog entry.**

Add `/internal/iam/v1/platform/authorize` to `iam-internal.yaml` with `security: platformServiceMtls`, required `Authorization` header, a JSON request containing `permissionCode`, and these responses:

```yaml
'200':
  description: Active platform authorization
'401':
  description: Invalid platform access token
'403':
  description: Platform permission denied
'503':
  description: IAM dependency unavailable
```

Define `PlatformAuthorizationRequest` with required `permissionCode` and `PlatformAuthorizationResponse` with required positive `userId` and `permissionCode`. Keep the existing `gatewayMtls` membership operation unchanged. Add the new operation to `API-CATALOG.yaml` with `tenantScoped: false`, `command: false`, and `hasRequestBody: true`. Add traceability row:

```csv
IAM-PLATFORM-AUTH-001,Platform authorization is independent from tenant membership,iam/tenant,"platform_user,platform_role,platform_permission,platform_role_permission,platform_user_role",iam-internal.yaml,,real MySQL allow/deny plus IAM outage fail-closed,TASK-003
```

- [ ] **Step 4: Validate contract files before Java work.**

Run:

```powershell
python -c "import yaml; [yaml.safe_load(open(p, encoding='utf-8')) for p in ['spec/alop-v7.0/04-openapi/iam-internal.yaml','spec/alop-v7.0/10-registries/permissions.yaml','spec/alop-v7.0/11-codegen/API-CATALOG.yaml']]; print('YAML OK')"
python scripts/validate_project.py
git diff --check
```

Expected: YAML OK, project validation PASS, and no whitespace errors. The existing IAM V1 DDL and membership OpenAPI must remain unchanged.

## Task 2: Implement IAM platform authorization as a test-first application core

**Files:**

- Create: `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/dto/PlatformAuthorizationRequest.java`
- Create: `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/dto/PlatformAuthorizationResponse.java`
- Create: `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/dto/PlatformAuthorizationError.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/application/service/PlatformAuthorizationDecision.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/application/service/PlatformAuthorizationService.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/application/service/JwtPlatformAuthorizer.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/infrastructure/persistence/repository/JdbcPlatformAuthorizationAuthority.java`
- Create: `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/application/service/JwtPlatformAuthorizerTest.java`
- Create: `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/infrastructure/persistence/repository/JdbcPlatformAuthorizationAuthorityTest.java`

- [ ] **Step 1: Write the failing JWT/application tests.**

Create `JwtPlatformAuthorizerTest` with a stub `JwtDecoder` and authority. The required behavior is:

```java
@Test
void acceptsActivePlatformTokenWithoutTenantClaims() throws Exception {
    JwtPlatformAuthorizer authorizer = new JwtPlatformAuthorizer(
            jwtDecoderFor("42", "https://issuer.example.test/iam", "alop-admin"),
            (userId, permission) -> {},
            "https://issuer.example.test/iam", "alop-admin", fixedClock());

    assertEquals(new PlatformAuthorizationDecision(42, "platform:tenant:create"),
            authorizer.authorize("signed-token", "platform:tenant:create"));
}

@Test
void rejectsMissingTenantFreeTokenClaimsOnlyWhenTheyAreMalformed() {
    JwtPlatformAuthorizer authorizer = authorizerForJwt("42", null, null);
    assertThrows(BusinessException.class,
            () -> authorizer.authorize("signed-token", "platform:tenant:create"));
}

@Test
void rejectsWrongIssuer() {
    assertErrorCode(authorizerForJwt("42", "https://wrong.example", "alop-admin"),
            "signed-token", "platform:tenant:create", "TENANT_CONTEXT_REQUIRED");
}

@Test
void rejectsWrongAudience() {
    assertErrorCode(authorizerForJwt("42", "https://issuer.example.test/iam", "other-api"),
            "signed-token", "platform:tenant:create", "TENANT_CONTEXT_REQUIRED");
}

@Test
void rejectsExpiredToken() {
    assertErrorCode(expiredAuthorizer(),
            "signed-token", "platform:tenant:create", "TENANT_CONTEXT_REQUIRED");
}

@Test
void rejectsNonRs256Token() {
    assertErrorCode(nonRs256Authorizer(),
            "signed-token", "platform:tenant:create", "TENANT_CONTEXT_REQUIRED");
}

@Test
void rejectsPermissionDenied() {
    JwtPlatformAuthorizer authorizer = authorizerWithDeniedAuthority();
    assertErrorCode(authorizer, "signed-token", "platform:tenant:create",
            "TENANT_ACCESS_DENIED");
}
```

Define the referenced test helpers in the same test class so each case constructs a concrete `Jwt` with the stated claim or algorithm and the assertion checks the exact `BusinessException.error().code()` value.

- [ ] **Step 2: Run focused RED tests.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am -Dtest=JwtPlatformAuthorizerTest -Dsurefire.failIfNoSpecifiedTests=false test -B
```

Expected: compilation/test failure because the new decision, service, authorizer, and authority types do not exist.

- [ ] **Step 3: Add the smallest application and repository contracts.**

Use these exact signatures:

```java
public record PlatformAuthorizationDecision(long userId, String permissionCode) {
    public PlatformAuthorizationDecision {
        if (userId <= 0 || permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("Platform authorization decision is invalid");
        }
    }
}

@FunctionalInterface
public interface PlatformAuthorizationService {
    PlatformAuthorizationDecision authorize(String accessToken, String permissionCode)
            throws SQLException;
}

public final class JdbcPlatformAuthorizationAuthority {
    private final DataSource dataSource;

    public JdbcPlatformAuthorizationAuthority(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    private static final String SQL = """
            SELECT 1
              FROM platform_user AS u
              JOIN platform_user_role AS ur ON ur.user_id = u.id AND ur.status = 'ACTIVE'
              JOIN platform_role AS r ON r.id = ur.role_id AND r.status = 'ACTIVE'
              JOIN platform_role_permission AS rp ON rp.role_id = r.id
              JOIN platform_permission AS p ON p.id = rp.permission_id AND p.status = 'ACTIVE'
             WHERE u.id = ? AND u.status = 'ACTIVE' AND p.permission_code = ?
             LIMIT 1
            """;

    public void requirePermission(long userId, String permissionCode) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);
                }
            }
        }
    }
}
```

Use this SQL in the repository; the implementation must bind parameter 1 to `userId` and parameter 2 to `permissionCode`:

```sql
SELECT 1
  FROM platform_user AS u
  JOIN platform_user_role AS ur ON ur.user_id = u.id AND ur.status = 'ACTIVE'
  JOIN platform_role AS r ON r.id = ur.role_id AND r.status = 'ACTIVE'
  JOIN platform_role_permission AS rp ON rp.role_id = r.id
  JOIN platform_permission AS p ON p.id = rp.permission_id AND p.status = 'ACTIVE'
 WHERE u.id = ? AND u.status = 'ACTIVE' AND p.permission_code = ?
 LIMIT 1
```

It must never read `tenant_user_membership`, `user_role`, or `role`.

- [ ] **Step 4: Implement JWT validation without tenant claims.**

`JwtPlatformAuthorizer.authorize` must:

1. Reject blank token or blank permission with `TENANT_CONTEXT_REQUIRED`.
2. Decode with the trusted `JwtDecoder`; map `JwtException` to `TENANT_CONTEXT_REQUIRED`.
3. Require `alg=RS256`, exact issuer, configured audience, non-expired `exp`, and a valid `sub` greater than zero.
4. Do not read or require `tenantId` or `membershipId`.
5. Call `JdbcPlatformAuthorizationAuthority.requirePermission(userId, permissionCode)`.
6. Return `new PlatformAuthorizationDecision(userId, permissionCode)` only after the authority returns.

Run the focused test again and expect PASS.

- [ ] **Step 5: Add repository unit tests for the SQL contract.**

Use a proxy/mock `DataSource` or the existing JDBC test style to assert the repository binds `userId` and `permissionCode`, returns for an active row, and throws `TENANT_ACCESS_DENIED` when the result set is empty. Include a test proving the SQL string contains all five platform tables and none of `tenant_user_membership`, `user_role`, or `role`.

Run:

```powershell
mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am -Dtest=JwtPlatformAuthorizerTest,JdbcPlatformAuthorizationAuthorityTest -Dsurefire.failIfNoSpecifiedTests=false test -B
```

Expected: PASS.

## Task 3: Expose the IAM internal API and wire required configuration

**Files:**

- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/interfaces/internal/PlatformAuthorizationController.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/infrastructure/config/IamPlatformAuthorizationConfiguration.java`
- Create: `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/interfaces/internal/PlatformAuthorizationControllerTest.java`
- Create: `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/infrastructure/config/IamPlatformAuthorizationConfigurationTest.java`
- Modify: `backend/alop-iam/alop-iam-server/src/main/resources/application.yml`

- [ ] **Step 1: Write failing controller tests.**

Create tests for these exact mappings:

| Input/service result | HTTP | Body code |
| --- | --- | --- |
| missing or malformed `Authorization` | 401 | `TENANT_CONTEXT_REQUIRED` |
| valid service call and active permission | 200 | `PlatformAuthorizationResponse(42, "platform:tenant:create")` |
| `BusinessException(TENANT_ACCESS_DENIED)` | 403 | `TENANT_ACCESS_DENIED` |
| `SQLException` | 503 | `IAM_UNAVAILABLE` |
| blank `permissionCode` | 400 | `TENANT_INVALID_REQUEST` |

The controller must call the service only after both bearer-token and body validation pass.

- [ ] **Step 2: Run RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am -Dtest=PlatformAuthorizationControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -B
```

Expected: compilation failure because the controller and DTOs do not exist.

- [ ] **Step 3: Implement DTOs and controller.**

Use records with required validation at the controller boundary:

```java
public record PlatformAuthorizationRequest(String permissionCode) {}

public record PlatformAuthorizationResponse(long userId, String permissionCode) {}

public record PlatformAuthorizationError(String code) {}
```

The controller path is `/internal/iam/v1/platform`, method is `POST /authorize`, and the method receives `Authorization` plus `PlatformAuthorizationRequest`. It extracts exactly one bearer token, dispatches `service.authorize(token, request.permissionCode())`, maps `BusinessException` using the existing `ErrorDefinition.httpStatus()`, and maps `SQLException` to HTTP 503 with `IAM_UNAVAILABLE`. It must never log the token.

- [ ] **Step 4: Add IAM configuration and startup fail-fast tests.**

`IamPlatformAuthorizationConfiguration` must reuse the existing required `JwtDecoder` bean, bind `JdbcPlatformAuthorizationAuthority`, and expose a `PlatformAuthorizationService` backed by `JwtPlatformAuthorizer`. Keep issuer and audience required and use the same `Clock.systemUTC()` convention as membership verification.

Add startup assertions that missing `alop.security.jwt.jwk-set-uri`, issuer, or audience prevents the IAM context from starting. Keep `server.ssl.client-auth=need`; the internal endpoint is not a public endpoint. Do not add a default/mock authorization bean.

Add the platform operation configuration to `application.yml` only if a distinct property is required; the JWT properties remain under `alop.security.jwt`. Do not duplicate secrets or certificates.

- [ ] **Step 5: Run IAM unit tests and project validation.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am test -B
python scripts/validate_project.py
```

Expected: all existing IAM membership tests plus new platform authorization/controller/configuration tests pass, and project validation reports PASS.

## Task 4: Add the Tenant HTTPS/mTLS authorization client test-first

**Files:**

- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/PlatformAuthorizationPort.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/IamPlatformAuthorizationClient.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/IamPlatformAuthorizationClientProperties.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/config/IamPlatformAuthorizationClientConfiguration.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/infrastructure/security/IamPlatformAuthorizationClientTest.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/infrastructure/config/IamPlatformAuthorizationClientConfigurationTest.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/main/resources/application.yml`
- Delete: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/PlatformActorResolver.java`
- Delete: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/security/TenantCreateAuthorizer.java`
- Delete: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/FailClosedPlatformActorResolver.java`
- Delete: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/infrastructure/security/FailClosedTenantCreateAuthorizer.java`

- [ ] **Step 1: Write the failing client and property tests.**

Define this application port:

```java
public interface PlatformAuthorizationPort {
    PlatformActor authorize(String authorization, String permissionCode);
}
```

Write client tests using a local `MockWebServer`-style HTTP server or a stub `RestClient` request factory. Required cases:

```java
@Test
void maps200ToPlatformActorAndSendsBearerAndPermission() {}

@Test
void maps401And403ToTenantAccessDenied() {}

@Test
void mapsTimeout5xxAndMalformed200ToTenantCreateNotReady() {}

@Test
void propertiesRequireHttpsBaseUrlSslBundleAndBoundedTimeout() {}
```

Each test must assert the request path `/internal/iam/v1/platform/authorize`, `Authorization: Bearer ...`, JSON permission code, and no fallback actor.

- [ ] **Step 2: Run RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=IamPlatformAuthorizationClientTest,IamPlatformAuthorizationClientConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -B
```

Expected: compilation failure because the new port, client, properties, and configuration do not exist.

- [ ] **Step 3: Implement the bounded properties and mTLS RestClient.**

Use `@ConfigurationProperties(prefix="alop.security.iam.platform")` with:

```java
private URI baseUrl;
private String sslBundle;
private Duration timeout = Duration.ofMillis(500);
```

`validate()` must require an HTTPS host, a nonblank SSL bundle, and a timeout between 1 ms and 5 s. Build `java.net.http.HttpClient` from the configured Spring `SslBundle`, set its connect timeout, wrap it in `JdkClientHttpRequestFactory`, and build a Spring `RestClient` with the configured base URL. Do not use a plain HTTP client or direct IAM JDBC access.

- [ ] **Step 4: Implement fail-closed status mapping.**

`IamPlatformAuthorizationClient.authorize` must reject a missing/non-bearer header with `TENANT_ACCESS_DENIED` before network I/O. Send `PlatformAuthorizationRequest(permissionCode)` and validate that the 200 response has a positive `userId` and exactly the requested permission code. Map 401/403 to `TENANT_ACCESS_DENIED`; map timeout, connection errors, 5xx, empty response, and malformed 200 to `TENANT_CREATE_NOT_READY`. Never log or cache the token.

- [ ] **Step 5: Wire properties and run client tests.**

Add required production properties:

```yaml
alop:
  security:
    iam:
      platform:
        base-url: ${ALOP_IAM_BASE_URL:}
        ssl-bundle: ${ALOP_IAM_CLIENT_SSL_BUNDLE:}
        timeout: ${ALOP_IAM_PLATFORM_AUTH_TIMEOUT:500ms}
```

The configuration must fail startup when the URL or SSL bundle is missing. Run the focused tests and expect PASS.

## Task 5: Replace the Tenant v2 fail-closed adapters with the real authorization port

**Files:**

- Modify: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/TenantCreateV2Controller.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/interfaces/rest/TenantCreateV2ControllerTest.java`
- Modify: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/application/security/PlatformActorTest.java`
- Delete: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/infrastructure/security/FailClosedTenantCreateSecurityTest.java`

- [ ] **Step 1: Write the failing controller boundary assertions.**

Change the controller constructor to accept one `PlatformAuthorizationPort`. Add an optional `Authorization` header parameter. Add tests for the exact flow:

```java
@Test
void validAuthorizedRequestStillStopsAtReadinessBoundary() {}

@Test
void iamPermissionDenialReturns403BeforeCreateService() {}

@Test
void iamUnavailableReturns503BeforeCreateService() {}

@Test
void invalidBodyDoesNotCallIam() {}
```

The existing behavior remains: valid request plus authorized platform actor returns HTTP 503 with `TENANT_CREATE_NOT_READY`; this task must not call `CreateTenantService` or write a Tenant row.

- [ ] **Step 2: Run RED.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=TenantCreateV2ControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -B
```

Expected: compilation failure after the constructor and header contract change.

- [ ] **Step 3: Implement the minimal controller change.**

The method signature must include:

```java
@RequestHeader(value = "Authorization", required = false) String authorization
```

After body/header validation, call:

```java
PlatformActor actor = authorizationPort.authorize(
        authorization, "platform:tenant:create");
```

Do not use `actor.userId()` for `adminUserId`; retain `adminUserId` only as the target administrator field. Return `TENANT_CREATE_NOT_READY` immediately after the authorization decision. Preserve existing 400 validation and 403 error mapping.

- [ ] **Step 4: Run Tenant tests and verify no create call.**

Run:

```powershell
mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test -B
```

Expected: all existing Tenant lifecycle/create-service tests plus the updated REST/client tests pass. The test fixture must prove `CreateTenantService` is never invoked by the v2 controller in this slice.

## Task 6: Add real MySQL/RSA integration coverage and update evidence

**Files:**

- Create: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationIamPlatformAuthorizationIT.java`
- Modify: `scripts/verify_rabbit_mysql_docker.py`
- Modify: `docs/project-management/PROGRESS.md`
- Create: `docs/development/implementation/TASK-003-IAM-PLATFORM-AUTHORIZATION.md`

- [ ] **Step 1: Write the failing real integration test.**

Create a disposable MySQL integration class using the existing `IntegrationIamMembershipIT` pattern. In `@BeforeEach`, clear platform assignments/catalogs and seed:

```sql
INSERT INTO platform_user(id, username, password_hash, status)
VALUES (42, 'user42', 'hash', 'ACTIVE'), (43, 'user43', 'hash', 'DISABLED');
INSERT INTO platform_permission(id, permission_code, permission_name, module_code, status)
VALUES (8001, 'platform:tenant:create', 'Create tenant', 'platform', 'ACTIVE'),
       (8002, 'platform:tenant:suspend', 'Suspend tenant', 'platform', 'ACTIVE');
INSERT INTO platform_role(id, role_code, role_name, status, risk_level)
VALUES (8101, 'PLATFORM_OPERATOR', 'Platform operator', 'ACTIVE', 3),
       (8102, 'PLATFORM_DISABLED', 'Disabled platform role', 'DISABLED', 3);
INSERT INTO platform_role_permission(role_id, permission_id)
VALUES (8101, 8001), (8101, 8002), (8102, 8001);
INSERT INTO platform_user_role(user_id, role_id, status)
VALUES (42, 8101, 'ACTIVE'), (43, 8101, 'ACTIVE');
```

Sign RS256 tokens containing only `sub`, `iss`, `aud`, `iat`, `nbf`, and `exp`. Assert allow for user 42, deny after setting `platform_user_role.status='DISABLED'`, deny for disabled user 43, deny for an unknown permission, and deny a user that has only a tenant membership but no platform assignment. Assert JWT issuer/audience/expiry/signature failures remain denied.

- [ ] **Step 2: Run RED before adding the migration to the harness.**

Run:

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: the new integration test is not yet included or fails because the new IAM migration/table setup is absent. Capture the failure; do not weaken the test.

- [ ] **Step 3: Add V2 IAM migration to the Docker harness and test list.**

Update `scripts/verify_rabbit_mysql_docker.py` so `IAM_SQL` is a list containing `iam/V1__init.sql` and `iam/V2__platform_authorization.sql`, apply both in order, and include `IntegrationIamPlatformAuthorizationIT` in the Maven `-Dtest` list. Keep existing IAM membership and Tenant integration tests unchanged.

- [ ] **Step 4: Run the complete real verification.**

Run:

```powershell
python scripts/verify_rabbit_mysql_docker.py
```

Expected: the existing integration classes and `IntegrationIamPlatformAuthorizationIT` pass with zero failures/errors/skips, and the script removes its temporary containers. Also run:

```powershell
python -m py_compile scripts/verify_rabbit_mysql_docker.py
git diff --check
```

- [ ] **Step 5: Record implementation evidence and progress.**

Create `docs/development/implementation/TASK-003-IAM-PLATFORM-AUTHORIZATION.md` with the exact migration, endpoint, allow/deny cases, commands, and observed counts. Append a dated section to `docs/project-management/PROGRESS.md` stating:

```text
TASK-003 platform authorization is implemented with independent IAM role/permission tables and a fail-closed mTLS internal authorization boundary. Tenant v2 remains intentionally not-ready after authorization because admin provisioning, TenantCreated Outbox registration, and the remaining initialization Saga are separate work.
```

Do not claim Tenant v2 creation success or Administrator membership provisioning in this evidence.

## Final verification checklist

- [ ] `mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am test -B`
- [ ] `mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test -B`
- [ ] `mvn -f backend/pom.xml -pl alop-framework/alop-test-support -am -DskipTests test-compile -B`
- [ ] `python scripts/verify_rabbit_mysql_docker.py`
- [ ] `python scripts/validate_project.py`
- [ ] YAML/CSV parse passes for changed specs
- [ ] `python -m py_compile scripts/verify_rabbit_mysql_docker.py`
- [ ] `git diff --check`
- [ ] `git diff --quiet -- spec/alop-v7.0/03-database/flyway/iam/V1__init.sql spec/alop-v7.0/03-database/flyway/tenant/V1__init.sql spec/alop-v7.0/04-openapi/tenant.yaml`
- [ ] No temporary Docker containers remain
- [ ] Only TASK-003 files are staged if a user-requested commit is made
