# TASK-001 JWT/IAM/Gateway Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved TASK-001 live IAM Membership verification bridge so Gateway admission is based on IAM-owned, real-time user/membership state and fails closed on invalid or unavailable authorization.

**Architecture:** Add a versioned internal IAM verification contract over mTLS. IAM revalidates the original RS256 access token and reads its own `platform_user` and `tenant_user_membership` tables; Gateway calls that endpoint through the existing `GatewayMembershipAuthority` SPI, with bounded concurrency, a 500ms response timeout, no retries, and explicit 401/403/503 mapping. No service reads another service's database and no frozen business DDL changes.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Web MVC, Spring WebFlux/WebClient, Spring Cloud Gateway, Spring Security OAuth2 JOSE, Spring Boot SSL bundles, MySQL 8, Maven, JUnit 5.

---

## Task 1: Freeze and register the internal verification contract

**Files:**
- Modify: `docs/architecture/ADR-PROPOSAL-002-JWT-IAM-MEMBERSHIP-VERIFICATION.md`
- Create: `spec/alop-v7.0/04-openapi/iam-internal.yaml`
- Modify: `spec/alop-v7.0/11-codegen/API-CATALOG.yaml`
- Modify: `spec/alop-v7.0/11-codegen/TRACEABILITY-MATRIX.csv`
- Test: `scripts/validate_project.py` if present; otherwise run the repository's documented spec validation command and a YAML parse check.

- [ ] **Step 1: Record the approved decision in ADR-002 without changing the public IAM API.**

Keep the existing public `spec/alop-v7.0/04-openapi/iam.yaml` unchanged. Update ADR-002 with:

```text
Status: ACCEPTED for the internal bridge contract; public login/token lifecycle remains out of scope.
Endpoint: POST /internal/iam/v1/membership/verify
Transport: mTLS; client identity is alop-gateway; raw user token remains in Authorization header.
Request: Authorization + X-Selected-Tenant-Id; no JSON body.
Response: userId, tenantId, membershipId.
Admission: IAM independently verifies RS256 JWT and live ACTIVE user/membership state.
Failure: 401 TENANT_CONTEXT_REQUIRED, 403 TENANT_ACCESS_DENIED, transport/DB failure -> 5xx.
```

Do not remove the explicit note that Tenant lifecycle status belongs to TASK-002 and that this does not complete TASK-001.

- [ ] **Step 2: Add the internal OpenAPI contract.**

Create `iam-internal.yaml` with the following complete shape:

```yaml
openapi: 3.0.3
info:
  title: ALOP IAM Internal API
  version: 7.0.0
  description: Internal service-to-service IAM membership verification.
servers:
  - url: /
paths:
  /internal/iam/v1/membership/verify:
    post:
      operationId: verifyMembershipForGateway
      security:
        - gatewayMtls: []
      parameters:
        - $ref: '#/components/parameters/Authorization'
        - $ref: '#/components/parameters/SelectedTenantId'
      responses:
        '200':
          description: Active membership identity
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MembershipVerificationResponse'
        '401':
          description: Invalid access token
        '403':
          description: Membership or tenant mismatch
        '503':
          description: IAM dependency unavailable
components:
  securitySchemes:
    gatewayMtls:
      type: mutualTLS
  parameters:
    Authorization:
      name: Authorization
      in: header
      required: true
      schema:
        type: string
        pattern: '^Bearer .+$'
    SelectedTenantId:
      name: X-Selected-Tenant-Id
      in: header
      required: true
      schema:
        type: integer
        format: int64
        minimum: 1
  schemas:
    MembershipVerificationResponse:
      type: object
      required: [userId, tenantId, membershipId]
      properties:
        userId: { type: integer, format: int64, minimum: 1 }
        tenantId: { type: integer, format: int64, minimum: 1 }
        membershipId: { type: integer, format: int64, minimum: 1 }
```

- [ ] **Step 3: Register the endpoint in the codegen catalog and traceability matrix.**

Append one catalog record with `file: 04-openapi/iam-internal.yaml`, `method: POST`, `path: /internal/iam/v1/membership/verify`, `operationId: verifyMembershipForGateway`, `command: false`, `tenantScoped: false`, `hasRequestBody: false`, and summary `Verify live IAM membership for Gateway admission`. Add one traceability row for live Membership-based tenant admission, pointing to the internal API, IAM tables, Gateway/IAM contract tests, and `TASK-001`.

- [ ] **Step 4: Validate the contract before Java changes.**

Run:

```powershell
python scripts/validate_project.py
git diff --check -- docs/architecture/ADR-PROPOSAL-002-JWT-IAM-MEMBERSHIP-VERIFICATION.md spec/alop-v7.0/04-openapi/iam-internal.yaml spec/alop-v7.0/11-codegen/API-CATALOG.yaml spec/alop-v7.0/11-codegen/TRACEABILITY-MATRIX.csv
```

Expected: repository validation passes; no frozen public `iam.yaml`/DDL file is modified; `git diff --check` is clean.

## Task 2: Add the IAM internal verification endpoint

**Files:**
- Create: `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/dto/MembershipVerificationResponse.java`
- Create: `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/dto/MembershipVerificationError.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/application/service/MembershipVerificationService.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/interfaces/internal/MembershipVerificationController.java`
- Create: `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/infrastructure/config/IamMembershipVerificationConfiguration.java`
- Modify: `backend/alop-iam/alop-iam-server/src/main/resources/application.yml`
- Create: `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/interfaces/internal/MembershipVerificationControllerTest.java`
- Test: `backend/alop-framework/alop-test-support/src/test/java/com/company/alop/framework/test/support/IntegrationIamMembershipIT.java`

- [ ] **Step 1: Write the failing DTO/controller tests.**

Use a fake `MembershipVerificationService` and call the controller directly so the tests do not require a live web server. The expected public methods are:

```java
// Java 8-compatible POJOs with a no-argument constructor, getters, and setters.
public final class MembershipVerificationResponse {
    private long userId;
    private long tenantId;
    private long membershipId;
}
public final class MembershipVerificationError {
    private String code;
}

public ResponseEntity<?> verify(String authorization, String selectedTenantHeader)
```

Cover these cases before implementation:

```java
assertEquals(HttpStatus.OK, controller.verify("Bearer signed", "100").getStatusCode());
assertEquals(HttpStatus.UNAUTHORIZED, controller.verify(null, "100").getStatusCode());
assertEquals(HttpStatus.UNAUTHORIZED, controller.verify("Basic signed", "100").getStatusCode());
assertEquals(HttpStatus.FORBIDDEN, controller.verify("Bearer signed", "200").getStatusCode());
assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.verify("Bearer signed", "100").getStatusCode());
```

Assert that the success body contains only `userId`, `tenantId`, and `membershipId` and that error bodies contain no token.

- [ ] **Step 2: Run the focused test to confirm it fails.**

Run:

```powershell
cd backend
mvn -pl alop-iam/alop-iam-server -am '-Dtest=MembershipVerificationControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

Expected: compilation/test failure because the DTOs, service, and controller do not yet exist.

- [ ] **Step 3: Implement the minimal IAM application path.**

Implement the following behavior without introducing new tables:

```java
public final class MembershipVerificationService {
    private final JwtMembershipAuthenticator authenticator;

    public TenantContext verify(String accessToken, long selectedTenantId) throws SQLException {
        return authenticator.authenticate(accessToken, selectedTenantId);
    }
}
```

The controller must:

1. Require exactly `Bearer <token>` and a positive decimal `X-Selected-Tenant-Id`.
2. Call the service with the original token string.
3. Map `TENANT_CONTEXT_REQUIRED` to 401, `TENANT_ACCESS_DENIED` to 403, and `SQLException` to 503.
4. Return `MembershipVerificationResponse` only on success.
5. Never log or echo the token.

`IamMembershipVerificationConfiguration` must create the IAM `JwtDecoder` only from an explicitly configured HTTPS JWKS URI and explicit issuer/audience, then wire the existing `JdbcMembershipAuthority` and `JwtMembershipAuthenticator`. Missing security configuration must fail startup rather than create an unsafe default.

- [ ] **Step 4: Require server-side mTLS configuration without hardcoded secrets.**

Add only configuration keys/documentation to `application.yml`; do not add certificates or private keys:

```yaml
alop:
  security:
    jwt:
      jwk-set-uri: ${ALOP_IAM_JWK_SET_URI:}
      issuer: ${ALOP_IAM_JWT_ISSUER:}
      audience: ${ALOP_IAM_JWT_AUDIENCE:}
server:
  ssl:
    client-auth: need
    bundle: ${ALOP_IAM_SERVER_SSL_BUNDLE:iam-server}
```

The deployment must provide the SSL bundle and trust only the Gateway client certificate; the application must not treat a normal request header as service identity.

- [ ] **Step 5: Run focused IAM tests and existing real-membership tests.**

Run:

```powershell
cd backend
mvn -pl alop-iam/alop-iam-server -am '-Dtest=MembershipVerificationControllerTest' test -B
cd ..
python scripts/verify_rabbit_mysql_docker.py
```

Expected: controller tests pass; the disposable MySQL suite still passes all existing `IntegrationIamMembershipIT` cases. The script must clean up its containers.

## Task 3: Implement the Gateway IAM client and fail-closed admission

**Files:**
- Create: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/GatewayMembershipAuthenticationException.java`
- Create: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/GatewayIamUnavailableException.java`
- Create: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/IamMembershipClientProperties.java`
- Create: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/IamMembershipAuthorityClient.java`
- Create: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/GatewayAuthRoutePolicy.java`
- Modify: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/GatewayJwtTenantFilter.java`
- Modify: `backend/alop-gateway/src/main/java/com/company/alop/gateway/security/GatewayTenantSecurityConfiguration.java`
- Modify: `backend/alop-gateway/pom.xml`
- Modify: `backend/alop-gateway/src/main/resources/application.yml`
- Create: `backend/alop-gateway/src/test/java/com/company/alop/gateway/security/IamMembershipAuthorityClientTest.java`
- Modify: `backend/alop-gateway/src/test/java/com/company/alop/gateway/security/GatewayJwtTenantFilterTest.java`
- Modify: `backend/alop-gateway/src/test/java/com/company/alop/gateway/security/GatewayStartupSecurityTest.java`

- [ ] **Step 1: Write failing client and filter tests.**

Use `WebClient.builder().exchangeFunction(...)` to return deterministic `ClientResponse` values; do not start an external server for unit tests. Cover:

```java
assertThat(client.verify(jwt, 100).block()).isEqualTo(new TenantContext(100, 1001, 42));
assertThatThrownBy(() -> client.verify(jwt, 100).block())
        .isInstanceOf(GatewayMembershipAuthenticationException.class); // IAM 401
assertThatThrownBy(() -> client.verify(jwt, 100).block())
        .isInstanceOf(BusinessException.class); // IAM 403 -> TENANT_ACCESS_DENIED
assertThatThrownBy(() -> client.verify(jwt, 100).block())
        .isInstanceOf(GatewayIamUnavailableException.class); // timeout/5xx
```

Add filter assertions that IAM 401 maps to 401, IAM 403 maps to 403, transport failure maps to 503, and the fake exchange function is called once even when the first call fails.

- [ ] **Step 2: Run the focused tests to confirm failure.**

Run:

```powershell
cd backend
mvn -pl alop-gateway -am '-Dtest=IamMembershipAuthorityClientTest,GatewayJwtTenantFilterTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

Expected: compilation/test failure because the client, properties, exception mapping, and new filter constructor path do not exist.

- [ ] **Step 3: Implement the client with bounded concurrency and no retry.**

The client must:

1. Reject a non-HTTPS IAM base URL and a missing SSL bundle name at configuration time.
2. Build a JDK `HttpClient` from the Spring `SslBundles` bundle and use `JdkClientHttpConnector` for `WebClient`.
3. POST to `/internal/iam/v1/membership/verify` with the original JWT value in `Authorization` and the selected tenant in `X-Selected-Tenant-Id`.
4. Use `timeout(Duration.ofMillis(500))`, `AtomicInteger` admission with a maximum of 128, and `doFinally` release.
5. Never call `retry`, never cache a positive response, and map response statuses exactly as the design specifies.
6. Validate the returned IDs are positive and match the selected tenant and the already verified JWT claims before constructing `TenantContext`.

The core mapping shape is:

```java
return permit()
        .then(webClient.post().uri("/internal/iam/v1/membership/verify")
            .headers(headers -> {
                headers.setBearerAuth(jwt.getTokenValue());
                headers.set("X-Selected-Tenant-Id", Long.toString(selectedTenantId));
            })
            .retrieve()
            .onStatus(status -> status.value() == 401,
                response -> Mono.error(new GatewayMembershipAuthenticationException()))
            .onStatus(status -> status.value() == 403,
                response -> Mono.error(new BusinessException(CommonErrors.TENANT_ACCESS_DENIED)))
            .onStatus(HttpStatusCode::is5xxServerError,
                response -> Mono.error(new GatewayIamUnavailableException()))
            .bodyToMono(MembershipVerificationResponse.class))
        .timeout(timeout)
        .map(response -> toTenantContext(response, jwt, selectedTenantId))
        .onErrorMap(TimeoutException.class, ignored -> new GatewayIamUnavailableException())
        .doFinally(ignored -> inFlight.decrementAndGet());
```

- [ ] **Step 4: Update the filter’s exception mapping and route policy.**

Keep decoder errors as 401. Map `GatewayMembershipAuthenticationException` to 401, existing `BusinessException` to 403, and all IAM transport/configuration failures to 503. Sanitize `X-User-Id`, `X-Membership-Id`, and `X-Alop-*` before both public-route and authenticated-route handling. Public routes must be an exact configured set; an unconfigured path still requires the full JWT + IAM check.

- [ ] **Step 5: Wire configuration and dependencies surgically.**

Add only the `alop-iam-api` dependency and any directly required Spring Web/WebClient dependency to the Gateway POM. Add configuration keys without secrets:

```yaml
alop:
  security:
    iam:
      base-url: ${ALOP_IAM_BASE_URL:}
      ssl-bundle: ${ALOP_IAM_CLIENT_SSL_BUNDLE:}
      verify-timeout: ${ALOP_IAM_VERIFY_TIMEOUT:500ms}
      max-in-flight: ${ALOP_IAM_VERIFY_MAX_IN_FLIGHT:128}
      public-paths: []
```

Update startup tests so missing HTTPS base URL, missing SSL bundle, issuer, audience, or authority configuration fails closed. Do not add a default/mock authority bean to production configuration.

- [ ] **Step 6: Run Gateway tests and compile the full affected reactor.**

Run:

```powershell
cd backend
mvn -pl alop-gateway -am test -B
```

Expected: all Gateway tests and existing `alop-common` tests pass; startup tests prove incomplete security configuration is rejected.

## Task 4: Evidence, documentation, and release-gate verification

**Files:**
- Modify: `backend/alop-gateway/README.md`
- Modify: `backend/alop-iam/alop-iam-server/README.md`
- Create: `docs/development/implementation/TASK-001-IAM-GATEWAY-BRIDGE.md`
- Modify: `docs/project-management/PROGRESS.md` by appending a dated evidence section only.

- [ ] **Step 1: Update README boundaries.**

Replace the current “no IAM bridge implementation exists” statement with the actual endpoint/client behavior, required HTTPS/mTLS/SSL-bundle configuration, 500ms timeout, 128 in-flight cap, no retry, and the explicit limitation that Tenant Core lifecycle, RBAC, DataScope, ACL, login, token issuance, and full E2E deployment remain incomplete.

- [ ] **Step 2: Write the implementation evidence report.**

Record exact changed files, the approved ADR-002 contract, test commands, pass/fail counts, Docker image/container cleanup, and any remaining blocker. State clearly that this is a TASK-001 security bridge slice, not full TASK-001 completion or V7 production readiness.

- [ ] **Step 3: Run the complete verification set.**

Run:

```powershell
python scripts/validate_project.py
python scripts/verify_integration_mysql_docker.py
python scripts/verify_rabbit_mysql_docker.py
cd backend
mvn -pl alop-gateway -am test -B
mvn -pl alop-iam/alop-iam-server -am test -B
git diff --check
```

Expected: validation and affected Maven suites pass; disposable containers are removed; any environment failure is reported as an environment failure rather than marked as a code pass.

- [ ] **Step 4: Review the final diff against the approved design.**

Run:

```powershell
git diff --name-only
git diff -- docs/architecture/ADR-PROPOSAL-002-JWT-IAM-MEMBERSHIP-VERIFICATION.md spec/alop-v7.0/04-openapi/iam-internal.yaml backend/alop-gateway backend/alop-iam/alop-iam-api backend/alop-iam/alop-iam-server docs/development/implementation/TASK-001-IAM-GATEWAY-BRIDGE.md
```

Confirm every changed line belongs to the approved bridge scope, no frozen public IAM OpenAPI/DDL was changed, no secrets were added, and existing unrelated dirty files remain untouched.
