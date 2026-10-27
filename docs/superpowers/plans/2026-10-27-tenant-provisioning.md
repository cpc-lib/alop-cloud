# Tenant v2 Provisioning Saga Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved Tenant v2 Outbox/Inbox provisioning Saga so an authorized request returns `202 CREATING`, IAM provisions an existing platform user with exactly the approved tenant-admin permissions, and Tenant becomes `ACTIVE` only after all required local initialization succeeds.

**Architecture:** Keep Tenant and IAM as separate bounded contexts. Tenant owns creation, baseline tenant resources, lifecycle transitions, and the final `tenant.tenant.created.v1` event; IAM owns membership, tenant role, and permission relationships. Cross-service effects use the existing `mq_outbox`, `mq_inbox`, and `integration_task` tables with a narrowly scoped internal event context for background provisioning.

**Tech Stack:** Java 21, Spring Boot, JDBC/MySQL, RabbitMQ Java client, Jackson, JUnit 5, Maven, JSON Schema, YAML/CSV registries, Docker/Testcontainers verification.

**Reference design:** `docs/superpowers/specs/2026-10-27-tenant-provisioning-design.md` (approved, commit `8ecfb6e`).

**Workspace rule:** The repository already contains unrelated dirty changes. Never reset, clean, reformat, or stage the whole repository. Each task stages only its own files and commits only its own change set.

---

## File map and ownership

Shared event execution:

- Create `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/messaging/InternalEventContext.java` as the immutable service-to-service execution marker.
- Modify `backend/alop-framework/alop-spring-boot-starter-outbox/src/main/java/com/company/alop/framework/outbox/JdbcOutboxStore.java` with the internal enqueue path while preserving the existing `TenantContext` path.
- Modify `backend/alop-framework/alop-spring-boot-starter-inbox/src/main/java/com/company/alop/framework/inbox/JdbcInboxDeduplicator.java` with the internal consume path while preserving user-membership validation.
- Modify `backend/alop-framework/alop-spring-boot-starter-mq/src/main/java/com/company/alop/framework/mq/RabbitInboxDeliveryHandler.java` to share envelope validation and support the internal resolver/effect path.

Tenant:

- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/command/CreateTenantCommand.java`, `CreateTenantResult.java`, `Tenant.java`, and `JdbcTenantRepository.java` for the complete command identity and accepted response.
- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/service/CreateTenantService.java` and `TenantCreateConfiguration.java` to write the request Outbox in the same transaction.
- Create focused provisioning policy/repository/consumer classes under `application/provisioning`, `infrastructure/persistence/repository`, and `interfaces/mq` rather than putting provisioning SQL in the controller.
- Modify `TenantCreateV2Controller.java` and create the `202` response DTO; update `tenant-v2.yaml` and its tests.

IAM:

- Add the framework MQ/Inbox/Outbox dependencies to `backend/alop-iam/alop-iam-server/pom.xml`.
- Create focused IAM provisioning application, event, repository, configuration, and MQ consumer classes under the existing package structure.
- Reuse `tenant_user_membership`, `role`, `permission`, `role_permission`, and `user_role`; do not add a second tenant-role model or direct writes from Tenant.

Contracts and verification:

- Create four schemas under `spec/alop-v7.0/05-events/schemas/`.
- Modify `spec/alop-v7.0/05-events/event-registry.yaml`, `registry.md`, `spec/alop-v7.0/10-registries/error-codes.yaml`, and the API/traceability registries only where the new behavior requires it.
- Extend `backend/alop-framework/alop-test-support` and the Rabbit/MySQL Docker verification script with real duplicate, retry, and success-path assertions.
- Add `docs/development/implementation/TASK-004-TENANT-PROVISIONING.md` and append verified progress to `docs/project-management/PROGRESS.md`.

Frozen files that must not be modified: IAM V1 DDL, Tenant V1 DDL, integration V1 DDL, existing membership-verification OpenAPI, and the existing platform-authorization contract.

## Task 1: Add the internal event execution path

**Files:**
- Create `backend/alop-framework/alop-common/src/main/java/com/company/alop/framework/common/messaging/InternalEventContext.java`.
- Create tests under `backend/alop-framework/alop-common/src/test/java/com/company/alop/framework/common/messaging/`.
- Modify `JdbcOutboxStore.java`, `JdbcInboxDeduplicator.java`, and `RabbitInboxDeliveryHandler.java`.
- Create focused tests under the corresponding `src/test/java` trees.

- [ ] **Step 1: Write failing context and boundary tests.** Assert that the internal context requires a nonblank service identity, that user `TenantContext` methods still reject a mismatched tenant, and that internal Outbox/Inbox methods accept a positive event tenant only through the internal context overload.
- [ ] **Step 2: Run the focused framework tests and verify failure.** From `alop-cloud`, run `mvn -f backend/pom.xml -pl alop-framework/alop-common,alop-framework/alop-spring-boot-starter-outbox,alop-framework/alop-spring-boot-starter-inbox,alop-framework/alop-spring-boot-starter-mq -am test`; expected result is compilation/test failure because the internal API is absent.
- [ ] **Step 3: Implement the smallest internal API.** Add an immutable `InternalEventContext(String serviceName)` and overloads equivalent to `enqueue(Connection, InternalEventContext, OutboxEvent)` and `consume(Connection, InternalEventContext, long, String, String, Long, SqlEffect)`. The overloads must validate explicit transactions, positive event tenant IDs, event identity, and service identity; they must not create or accept a fake membership context. Refactor Rabbit envelope parsing into one private method and add an internal resolver/effect path that ACKs only after the same-connection Inbox and business transaction commits.
- [ ] **Step 4: Run the focused framework tests and verify success.** Repeat the Maven command; expected result is PASS, including existing user-context tests unchanged.
- [ ] **Step 5: Commit only this task.** Use `git add` with the exact framework files and commit `feat: add trusted internal event context`.

## Task 2: Register and validate the provisioning event contracts

**Files:**
- Create `spec/alop-v7.0/05-events/schemas/tenant-provisioning-requested-v1.schema.json`.
- Create `spec/alop-v7.0/05-events/schemas/iam-tenant-provisioning-completed-v1.schema.json`.
- Create `spec/alop-v7.0/05-events/schemas/iam-tenant-provisioning-failed-v1.schema.json`.
- Create `spec/alop-v7.0/05-events/schemas/tenant-created-v1.schema.json`.
- Modify `spec/alop-v7.0/05-events/event-registry.yaml` and `spec/alop-v7.0/05-events/registry.md`.
- Create schema validation tests under `backend/alop-framework/alop-test-support/src/test/` or the existing project validation script if that is the established location.

- [ ] **Step 1: Write contract fixtures/tests first.** Add one valid envelope fixture for each event and invalid cases for missing `tenantId`, wrong `eventType`, missing required data, and extra data properties.
- [ ] **Step 2: Run the registry/schema validation and verify failure.** Run `python scripts/validate_project.py`; expected result is failure until the registry references and schema files exist.
- [ ] **Step 3: Add the four schemas.** Use the existing envelope style: `eventVersion` is `1.0`, `tenantId` is a positive numeric string, `aggregateType`/`aggregateId` are explicit, and `data` has `additionalProperties: false`. Keep failure diagnostics stable and non-sensitive.
- [ ] **Step 4: Register the events.** Add producers/consumers, at-least-once delivery, Inbox idempotency, and schema paths to YAML and mirror the entries in `registry.md`. Register `tenant.tenant.created.v1` as a new V7 event; do not add the retired V6 alias.
- [ ] **Step 5: Run validation and commit.** Run `python scripts/validate_project.py` plus JSON parsing of all four schemas; expected result is PASS. Commit only the contract files with `docs: register tenant provisioning events`.

## Task 3: Extend the Tenant command and create transaction

**Files:**
- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/command/CreateTenantCommand.java`.
- Modify `CreateTenantResult.java`, `Tenant.java`, `JdbcTenantRepository.java`, `CreateTenantService.java`, and `TenantCreateConfiguration.java`.
- Create `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/application/provisioning/TenantProvisioningRequested.java` and `TenantProvisioningEventPublisher.java`.
- Create tests beside the existing `CreateTenantServiceTest.java` and `IntegrationTenantCreateIT.java`.

- [ ] **Step 1: Write failing command/domain tests.** Assert that `adminUserId`, timezone, and currency participate in `canonicalForm()`, reject nonpositive/invalid values, and that a rehydrated Tenant preserves timezone/currency while `provisionSucceeded()` increments version exactly once.
- [ ] **Step 2: Run the Tenant unit tests and verify failure.** From `alop-cloud`, run `mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=CreateTenantServiceTest,TenantLifecycleTest test`; expected result is failure because the command/domain signatures do not yet contain the new fields.
- [ ] **Step 3: Implement command/domain persistence fields.** Extend the command and Tenant value state without changing the frozen Tenant table. Update SELECT/INSERT mapping to write the existing `tenant.timezone` and `tenant.currency` columns. Keep existing defaults only when the request explicitly omits optional values; validate supported timezone/currency formats at the application boundary.
- [ ] **Step 4: Write the failing local transaction test.** Extend `IntegrationTenantCreateIT` to assert one transaction contains the Tenant row, platform-scoped idempotency row, and one `tenant.provisioning.requested.v1` Outbox row; inject a failure before commit and assert none remain.
- [ ] **Step 5: Implement the request Outbox.** Build the canonical versioned envelope with aggregate `Tenant`, aggregate version `0`, positive event tenant ID, request/admin/package data, trace ID, and correlation ID. Call the internal `JdbcOutboxStore` path before completing the idempotency response. Store response code `TENANT_CREATE_ACCEPTED` and a `CREATING` result; replay the same response without generating another event.
- [ ] **Step 6: Run Tenant unit and MySQL tests.** From `alop-cloud`, run the focused unit command and `mvn -f backend/pom.xml -pl alop-framework/alop-test-support -am -Dtest=IntegrationTenantCreateIT test`; expected result is PASS with duplicate idempotency and rollback evidence.
- [ ] **Step 7: Commit the Tenant create transaction.** Stage only the command/domain/service/repository/configuration/event publisher and tests; commit `feat: enqueue tenant provisioning request`.

## Task 4: Implement IAM membership and tenant-admin provisioning

**Files:**
- Modify `backend/alop-iam/alop-iam-server/pom.xml` and `application.yml`.
- Create IAM API event payload records under `backend/alop-iam/alop-iam-api/src/main/java/com/company/alop/iam/api/event/`.
- Create `TenantProvisioningService.java`, `TenantProvisioningEventParser.java`, and the corresponding configuration under `backend/alop-iam/alop-iam-server/src/main/java/com/company/alop/iam/`.
- Create `JdbcTenantProvisioningRepository.java` under `infrastructure/persistence/repository`.
- Create the Rabbit consumer under `interfaces/mq` or `infrastructure/mq` using the shared internal handler.
- Create unit, repository, and consumer tests under `backend/alop-iam/alop-iam-server/src/test/java/com/company/alop/iam/`.

- [ ] **Step 1: Write failing IAM service tests.** Cover ACTIVE platform user success, missing user, disabled user, existing membership reuse, new membership creation, new `TENANT_ADMIN` with exactly the three approved permissions, exact-set reuse, and `TENANT_ADMIN_ROLE_CONFLICT` when existing role bindings contain extras.
- [ ] **Step 2: Run the IAM focused tests and verify failure.** From `alop-cloud`, run `mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am test`; expected result is failure because the provisioning service and repositories do not exist.
- [ ] **Step 3: Implement the IAM local transaction.** Parse only the versioned event data; query `platform_user` for `ACTIVE`; insert/reuse `tenant_user_membership` by `(tenant_id,user_id)`; insert/reuse `role` by `(tenant_id,'TENANT_ADMIN')`; compare active `role_permission` codes before reuse; insert the three required permission links and `user_role` link idempotently. Never delete extra role permissions automatically.
- [ ] **Step 4: Add completion/failure publishing.** On success, write `iam.tenant.provisioning.completed.v1` to Outbox in the same transaction. On deterministic IAM failure, write `iam.tenant.provisioning.failed.v1` with a stable code and retryable flag and insert/update an `integration_task` with `task_type='TENANT_PROVISIONING'`, `business_type='TENANT'`, `business_id=tenantId`, `event_id`, `manual_required=1`; do not leak SQL or user-existence details. On transient SQL failure, roll back Inbox and let broker retry.
- [ ] **Step 5: Wire the consumer.** Add the queue/group configuration, use the internal event context, require Inbox before business effects, and ACK only after IAM effects plus completion/failure Outbox commit. A repeated event must return the processed Inbox path without a second event.
- [ ] **Step 6: Run IAM tests and commit.** Repeat the Maven command and commit only IAM changes with `feat: provision tenant admin in iam`.

## Task 5: Implement Tenant completion and baseline initialization

**Files:**
- Create `TenantProvisioningPolicy.java` and `TenantProvisioningProperties.java` under the Tenant application/configuration packages.
- Create `JdbcTenantProvisioningRepository.java` under Tenant infrastructure persistence.
- Create `TenantProvisioningCompletionService.java`, event parsers, and the MQ consumer under Tenant application/interfaces.
- Modify `TenantCreateConfiguration.java`, Tenant `application.yml`, and the Tenant server POM for framework MQ/Inbox/Outbox dependencies.
- Create tests for policy, repository, completion service, and consumer.

- [ ] **Step 1: Write failing policy/repository tests.** Assert DB-default quota insertion, request timezone/currency persistence, capability-declared feature insertion only, route insertion from explicit properties, and failure when route properties are missing.
- [ ] **Step 2: Run the focused Tenant tests and verify failure.** From `alop-cloud`, run `mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test`; expected result is failure because the policy and repository are absent.
- [ ] **Step 3: Implement the explicit provisioning policy.** Bind `alop.tenant.provisioning.route-type`, `datasource-key`, `region`, and `namespace-prefix`; reject missing route type or datasource key at provisioning time. Read the package capability JSON only for an explicit `features` array. Omit generic `tenant_config`, empty branding, and `support_session` writes from this slice.
- [ ] **Step 4: Write the failing completion tests.** Assert matching `tenantCreateRequestId` performs baseline initialization, transitions `CREATING -> ACTIVE`, writes one `tenant.tenant.created.v1` Outbox, and rejects mismatched request IDs or non-CREATING states without mutation.
- [ ] **Step 5: Implement the completion transaction.** Consume through Inbox using the internal event path, lock Tenant by ID, verify the request ID and current status, initialize quota/feature/route, call `Tenant.provisionSucceeded()`, persist the lifecycle update, enqueue `tenant.tenant.created.v1`, mark Inbox processed, and commit. Use the existing `integration_task` columns for deterministic failure/manual handling.
- [ ] **Step 6: Run unit/MySQL tests and commit.** Run the focused Tenant tests and the test-support integration test; expected result is PASS. Commit with `feat: complete tenant provisioning saga`.

## Task 6: Open the REST 202 boundary and update API metadata

**Files:**
- Modify `backend/alop-tenant/alop-tenant-server/src/main/java/com/company/alop/tenant/interfaces/rest/TenantCreateV2Controller.java` and `CreateTenantRequestV2.java`.
- Create `CreateTenantAcceptedResponse.java` if the existing result type cannot be exposed without coupling internal fields.
- Modify `TenantCreateV2ControllerTest.java`, `spec/alop-v7.0/04-openapi/tenant-v2.yaml`, `spec/alop-v7.0/11-codegen/API-CATALOG.yaml`, `spec/alop-v7.0/11-codegen/TRACEABILITY-MATRIX.csv`, and `spec/alop-v7.0/10-registries/error-codes.yaml` only for new stable codes/response metadata.

- [ ] **Step 1: Update failing REST tests.** Replace the readiness-boundary expectation with `202 Accepted` and `CREATING`; add tests for invalid timezone/currency, adminUserId hash conflict, IAM denial, and transport failure before any Tenant write.
- [ ] **Step 2: Run the controller tests and verify failure.** From `alop-cloud`, run `mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am -Dtest=TenantCreateV2ControllerTest test`; expected result is failure because the controller still returns `TENANT_CREATE_NOT_READY` and does not dispatch the service.
- [ ] **Step 3: Implement the controller boundary.** Preserve validation-before-authorization and the existing fail-closed platform authorization port. After authorization, dispatch the complete command to `CreateTenantService`, map the accepted result to HTTP 202, map idempotency conflict to its stable error, and never use the returned platform actor as `adminUserId`.
- [ ] **Step 4: Update OpenAPI and registries.** Document `202`, `CREATING`, the accepted response, stable error codes, and the status-query expectation without inventing an unimplemented endpoint.
- [ ] **Step 5: Run tests and commit.** Repeat the focused Maven command plus YAML/CSV validation; commit `feat: expose tenant create accepted response`.

## Task 7: Add real cross-service Docker verification

**Files:**
- Modify `backend/alop-framework/alop-test-support` integration fixtures and the existing Rabbit/MySQL verification script `scripts/verify_rabbit_mysql_docker.py`.
- Add or extend `IntegrationTenantProvisioningIT.java` with real migration and broker assertions.
- Update service `application.yml` only with non-secret test defaults required by the new consumers.

- [ ] **Step 1: Write the end-to-end assertions.** Cover one authorized request, outbox publication, IAM membership/role/three-permission rows, completion publication, Tenant baseline rows, `ACTIVE` state, and final TenantCreated event. Add duplicate delivery, disabled admin user, route-policy failure, and Outbox retry/dead task cases.
- [ ] **Step 2: Run the Docker test and verify failure.** Run `python scripts/verify_rabbit_mysql_docker.py`; expected result is failure until both services are wired into the queue and migrations/configuration are complete.
- [ ] **Step 3: Wire only the required test fixtures.** Seed one ACTIVE platform user and permissions, configure explicit route policy, run migrations in dependency order, and use the existing Rabbit publisher/consumer path. Do not alter the frozen migration files.
- [ ] **Step 4: Run until green and inspect cleanup.** Require all integration tests to pass, no skipped provisioning tests, and no leftover containers/volumes/processes. Capture exact counts and failure evidence in the implementation report.
- [ ] **Step 5: Commit verification changes.** Commit only test-support/runner changes with `test: verify tenant provisioning over rabbit mysql`.

## Task 8: Final verification and project handoff

**Files:**
- Create `docs/development/implementation/TASK-004-TENANT-PROVISIONING.md`.
- Modify `docs/project-management/PROGRESS.md`.
- Do not modify source files in this task.

- [ ] **Step 1: Run the complete verification set.** From `alop-cloud`, run:
  - `mvn -f backend/pom.xml -pl alop-iam/alop-iam-server -am test`
  - `mvn -f backend/pom.xml -pl alop-tenant/alop-tenant-server -am test`
  - `mvn -f backend/pom.xml -pl alop-framework/alop-test-support -am -DskipTests test-compile`
  - `python scripts/verify_rabbit_mysql_docker.py`
  - `python scripts/validate_project.py`
  - JSON schema/YAML/CSV parsing checks used by the repository
  - `git diff --check`
- [ ] **Step 2: Verify frozen boundaries.** Compare the final tree and confirm IAM V1, Tenant V1, integration V1, and existing membership-verification contract files are unchanged. Confirm no generated cache or container artifacts from this work are left behind.
- [ ] **Step 3: Write the implementation report.** Record changed files, migration order, API semantics, test counts, Docker evidence, retry/dead-task evidence, and the remaining non-goals. State explicitly that Tenant v2 provisioning is implemented but the project is not automatically declared production-ready.
- [ ] **Step 4: Update progress.** Append a TASK-004 section with checked items and links to the implementation report; preserve all previous progress entries.
- [ ] **Step 5: Perform final review before claiming completion.** Use the verification-before-completion checklist and request a focused read-only code review. Fix any finding, rerun the affected verification, and only then report the final status.

## Plan-level acceptance criteria

- The REST boundary no longer returns a fixed `TENANT_CREATE_NOT_READY` after successful platform authorization; it returns a durable `202 CREATING` result.
- Same-key/same-request replay creates no duplicate Tenant, Outbox event, membership, role, permissions, or final TenantCreated event.
- A missing or inactive `adminUserId` cannot produce `ACTIVE`; the failure is durable and actionable.
- `TENANT_ADMIN` has exactly the three approved permissions when provisioning succeeds; existing extra permissions are never silently deleted.
- `CREATING -> ACTIVE` and `tenant.tenant.created.v1` occur only after IAM completion and required Tenant baseline initialization commit together.
- Real MySQL/RabbitMQ verification passes, static validation passes, and all frozen V1 contracts remain unchanged.
