# PLATFORM-INTEGRATION DOMAIN SPEC

## 1. Bounded Context / Service
`cross-cutting`

## 2. Aggregate Roots
- `IntegrationTask`
- `ExportTask`
- `ImportTask`

## 3. Owned Tables
- `mq_outbox`
- `mq_inbox`
- `integration_task`
- `idempotency_record`
- `audit_log`
- `export_task`
- `import_task`

## 4. Commands
- `RetryIntegrationTask`
- `MarkIntegrationResolved`
- `CreateExport`
- `CreateImport`

## 5. Queries
- `ListIntegrationTasks`
- `GetAuditTrail`

## 6. Produced Events
- `IntegrationTaskCreated`

## 7. Permissions
- `platform:integration:retry`
- `audit:view`

## 8. Invariants
- `no silent DLQ`
- `audit is immutable for finance/security actions`
- `large export/import asynchronous`
- `outbox state is recoverable`

## 9. Transaction / Locking
- `claim token / SKIP LOCKED for outbox workers`

## 10. Idempotency
- `eventId+consumerGroup`
- `idempotency key scope`

## 11. Closure Condition
Every DEAD/UNKNOWN integration path has a visible task, owner, retry/compensation action, final resolution and audit.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.
