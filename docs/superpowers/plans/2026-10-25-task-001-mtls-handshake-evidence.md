# TASK-001 mTLS Handshake Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a repeatable real HTTPS/mTLS Gateway-to-IAM request test so the existing security bridge has deployment-grade transport evidence.

**Architecture:** Start an ephemeral JDK `HttpsServer` that requires a client certificate, generate disposable CA/server/client certificates with the local JDK `keytool`, construct a Spring Boot `SslBundle` from those stores, and call the existing `GatewayTenantSecurityConfiguration.iamMembershipWebClient` plus `IamMembershipAuthorityClient`. The test must assert the request path, bearer token, selected tenant, and the server-observed client certificate identity. No production code, public API, DDL, or credentials change is needed unless the test exposes a real defect.

**Tech Stack:** Java 21, JUnit 5, JDK `com.sun.net.httpserver.HttpsServer`, Spring Boot 3.5 `SslBundle`, Spring WebFlux `WebClient`, Maven.

---

### Task 1: Add the real mTLS integration test

**Files:**
- Create: `backend/alop-gateway/src/test/java/com/company/alop/gateway/security/GatewayIamMtlsHandshakeTest.java`

- [x] **Step 1: Write the test before changing production code.**

The test must:

1. Create a temporary directory and use the JDK `keytool` executable to generate a CA, a `localhost` server certificate, and an `alop-gateway` client certificate.
2. Build server and client `SSLContext` instances from the generated PKCS12 key/trust stores.
3. Start `HttpsServer` with `setNeedClientAuth(true)` and a handler at `/internal/iam/v1/membership/verify`.
4. Register the client stores in `SslBundle.of(SslStoreBundle.of(...))` and pass a `DefaultSslBundleRegistry` to `GatewayTenantSecurityConfiguration.iamMembershipWebClient`.
5. Call `IamMembershipAuthorityClient.verify` with a real `Jwt` and assert the returned `TenantContext`.
6. Assert the handler received `Authorization: Bearer signed`, `X-Selected-Tenant-Id: 100`, the exact internal path, and a peer principal containing `CN=alop-gateway`.

- [x] **Step 2: Run only the new test and record the result.**

Run from `backend`:

```powershell
mvn -pl alop-gateway -am '-Dtest=GatewayIamMtlsHandshakeTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

The test may pass without production changes because the bridge implementation already exists; in that case the test itself is the missing evidence and no production refactor is justified.

### Task 2: Repair only transport defects exposed by the test

**Files:**
- Modify only the production file named by the failing assertion, if any.
- Test: `backend/alop-gateway/src/test/java/com/company/alop/gateway/security/GatewayIamMtlsHandshakeTest.java`

- [x] **Step 1: If the test fails, classify the failure.** The initial failures were test-only import/API corrections; no production transport defect was exposed.

Acceptable production fixes are limited to the existing SSL bundle/WebClient construction or request transport. Do not add retries, token caches, bypasses, hardcoded certificates, or alternate authorization headers.

- [x] **Step 2: Re-run the focused test until it passes.**

Use the same Maven command and retain the failure as a regression assertion if a production defect was found.

### Task 3: Update implementation evidence

**Files:**
- Modify: `docs/development/implementation/TASK-001-IAM-GATEWAY-BRIDGE.md`
- Modify: `docs/project-management/PROGRESS.md` by appending one dated evidence section only.

- [x] **Step 1: Record the real handshake evidence.**

State that the test used ephemeral disposable certificates, required client authentication, observed the Gateway client certificate, and completed the IAM verification request. Keep production certificate provisioning, deployment configuration, and all unrelated TASK-001 capabilities explicitly open.

- [x] **Step 2: Run the affected regression set.**

```powershell
python scripts/validate_project.py
cd backend
mvn -pl alop-gateway -am test -B
cd ..
git diff --check
```

Expected: POM validation, all Gateway/common tests, and whitespace checks pass; no generated certificate files remain in the repository.

### Task 4: Review scope and workspace safety

- [x] **Step 1: Confirm frozen contract files are unchanged.**

```powershell
git diff -- spec/alop-v7.0/04-openapi/iam.yaml spec/alop-v7.0/03-database/flyway/iam/V1__init.sql
```

Expected: no diff.

- [x] **Step 2: Preserve unrelated dirty changes.**

Do not run `git add -A`, reset, clean, or delete unrelated files. Keep implementation and evidence changes uncommitted if they overlap the shared dirty worktree; only the focused plan may be committed separately if requested.
