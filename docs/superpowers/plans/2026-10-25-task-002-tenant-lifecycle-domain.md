# TASK-002 Tenant Lifecycle Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the pure Tenant aggregate state machine required by TASK-002 without introducing persistence, HTTP, event, or provisioning behavior.

**Architecture:** Keep lifecycle rules in a framework-free aggregate under `alop-tenant-server` and expose only explicit command methods. Use the existing common `BusinessException`/`ErrorDefinition` boundary for stable invalid-state and termination-blocked conflicts; do not add a generic status setter. Termination requires an explicit readiness value containing all closure checks from the Tenant Domain SPEC.

**Tech Stack:** Java 21, JUnit 5, existing `alop-common` error types, Maven.

---

### Task 1: Register the missing stable Tenant domain conflicts

**Files:**
- Modify: `spec/alop-v7.0/10-registries/error-codes.yaml`
- Modify: `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/error/CommonErrors.java`

- [x] **Step 1: Add exactly two non-breaking Tenant conflict definitions.**

Add `TENANT_INVALID_STATE` as HTTP 409/non-retryable with message `租户当前状态不允许该操作`, and `TENANT_TERMINATION_BLOCKED` as HTTP 409/non-retryable with message `租户仍有未完成业务闭环`. Keep all existing codes unchanged.

- [x] **Step 2: Parse the registry and compile common.**

```powershell
python scripts/validate_project.py
cd backend
mvn -pl alop-framework/alop-common -am test -B
```

Expected: registry/POM validation passes and the existing common tests remain green.

### Task 2: Add failing Tenant lifecycle domain tests

**Files:**
- Create: `backend/alop-tenant/alop-tenant-server/src/test/java/com/company/alop/tenant/domain/tenant/TenantLifecycleTest.java`

- [x] **Step 1: Write tests for the frozen state machine.**

Cover:

```java
Tenant tenant = Tenant.creating(1L, "acme", "Acme", "ENTERPRISE", 10L);
tenant.provisionSucceeded();
tenant.suspend();
tenant.resume();
tenant.expire();
tenant.requestTermination();
assertThrowsWithCode(CommonErrors.TENANT_TERMINATION_BLOCKED,
        () -> tenant.finalizeTermination(TenantTerminationReadiness.blocked()));
tenant.finalizeTermination(TenantTerminationReadiness.ready());
assertEquals(TenantStatus.TERMINATED, tenant.status());
```

Also assert invalid transitions return `TENANT_INVALID_STATE`, terminal tenants cannot reopen, only `ACTIVE` accepts business traffic, and each successful transition increments the aggregate version.

- [x] **Step 2: Run the focused test to confirm RED.**

```powershell
cd backend
mvn -pl alop-tenant/alop-tenant-server -am '-Dtest=TenantLifecycleTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

Expected: compilation failure because the Tenant aggregate, status enum, readiness value, and error constants do not yet exist.

### Task 3: Implement the minimal pure Tenant aggregate

**Files:**
- Modify: `backend/alop-tenant/alop-tenant-server/pom.xml`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/tenant/Tenant.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/tenant/TenantStatus.java`
- Create: `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/domain/tenant/TenantTerminationReadiness.java`

- [x] **Step 1: Add only the common and JUnit dependencies needed by the domain slice.**

The server module may depend on `alop-common`; tests may use JUnit 5 already managed by the parent. Do not add web, mapper, database, Redis, RabbitMQ, or Flowable dependencies for this slice.

- [x] **Step 2: Implement explicit state transitions.**

Use these transitions only:

```text
CREATING --provisionSucceeded--> ACTIVE
ACTIVE --suspend--> SUSPENDED
SUSPENDED --resume--> ACTIVE
ACTIVE --expire--> EXPIRED
ACTIVE|SUSPENDED|EXPIRED --requestTermination--> TERMINATING
TERMINATING --finalizeTermination(ready)--> TERMINATED
```

Invalid state transitions throw `BusinessException(CommonErrors.TENANT_INVALID_STATE)`. A non-ready termination throws `BusinessException(CommonErrors.TENANT_TERMINATION_BLOCKED)`. There is no generic status mutator and no automatic transition.

- [x] **Step 3: Run the focused test to confirm GREEN.**

```powershell
cd backend
mvn -pl alop-tenant/alop-tenant-server -am '-Dtest=TenantLifecycleTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -B
```

Expected: all Tenant lifecycle tests pass.

### Task 4: Record the bounded slice and run regression

**Files:**
- Modify: `backend/alop-tenant/MODULE-SPEC.md` only if its current text contradicts the implemented domain slice.
- Modify: `docs/project-management/PROGRESS.md` by appending a dated evidence section.
- Create: `docs/development/implementation/TASK-002-TENANT-LIFECYCLE-DOMAIN.md`

- [x] **Step 1: Record scope and remaining gaps.**

State that only the pure lifecycle aggregate is implemented. Explicitly leave repository/`FOR UPDATE`, CreateTenant idempotency, REST, Flyway, provisioning Saga, quota/config/feature/route, SupportSession, audit/metrics, and MySQL isolation unimplemented.

- [x] **Step 2: Run final verification.**

```powershell
python scripts/validate_project.py
cd backend
mvn -pl alop-tenant/alop-tenant-server -am test -B
cd ..
git diff --check
```

Expected: all commands exit 0; no frozen OpenAPI/DDL file is changed by this slice.

### Scope review

This slice does not claim TASK-002 or TASK-001 complete. It does not modify `tenant.yaml`, `03-database/flyway/tenant/V1__init.sql`, API catalog, event schemas, or application infrastructure.
