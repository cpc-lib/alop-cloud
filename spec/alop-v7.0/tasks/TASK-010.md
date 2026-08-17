# TASK-010 - Agreement Core V7.0

## V7.0 Frozen Baseline
- Mandatory master: `00-master/MASTER-SPEC-V7.0.md`.
- This task MUST follow `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `state-machines.yaml`, registries, DDL and OpenAPI/Event contracts.
- Do not add/merge bounded contexts or change frozen invariants without ADR.

## 1. Business Goal
实现生产级合同域：DRAFT -> 审批 -> 签署 -> 生效 -> 到期/终止 -> CLOSED 全生命周期、多资源 AgreementItem、签约快照不可变、Sign Saga 编排（ADR-006）、SIGNED != EFFECTIVE、EXPIRED != CLOSED 闭环门禁、续租优先权、变更走新记录、agreement-approval BPMN 集成、租户隔离与可靠事件。

## 2. Bounded Context
`alop-agreement`

## 3. Mandatory Input SPEC
- `00-master/MASTER-SPEC-V7.0.md` (§6.2 合同, §8 续租与退租闭环, §12 业务关闭定义, §13 技术红线)
- `02-domain/agreement/DOMAIN-SPEC.md`
- `02-domain/agreement/STATE-MACHINE.md`
- `02-domain/agreement/AGREEMENT-PARTY-SPEC.md` (context; implementation in TASK-026)
- `01-architecture/adr/ADR-006-agreement-sign-saga.md`
- `01-architecture/adr/ADR-010-agreement-closed.md`
- `01-architecture/adr/ADR-015-agreement-party.md` (context only)
- `03-database/DDL-CONTRACT.md`, `03-database/DATA-DICTIONARY.md`
- `03-database/flyway/agreement/V1__init.sql`
- `04-openapi/agreement.yaml`, `04-openapi/agreement-extensions.yaml`
- `05-events/registry.md`, `05-events/event-registry.yaml`
- `05-events/schemas/agreement-signed-v1.schema.json`, `agreement-expiring-v1.schema.json`
- `06-workflow/agreement-approval.bpmn20.xml`, `06-workflow/workflow-spec.md`
- `07-security/tenant-isolation.md`, `07-security/permissions.md`
- `08-tests/test-plan.md`, `08-tests/e2e-scenarios.md` (LEASE-001/002, MOVEOUT-001), `08-tests/concurrency.md`
- `12-test-data/e2e-lease-001.json`
- `examples/agreement-close-checklist.md`
- `11-codegen/TASK-CONTEXT-MATRIX.yaml`, `TRANSACTION-LOCK-MATRIX.yaml`, `IDEMPOTENCY-MATRIX.yaml`, `API-CATALOG.yaml`, `JOB-MATRIX.yaml`, `state-machines.yaml`
- `14-task-bundles/TASK-010/CONTEXT.md`

## 4. Aggregate / Entity
### Aggregates
- `Agreement`
- `AgreementChange`
- `RenewalPriority`
- `HandoverOrder`

### Entities
- `AgreementItem`
- `AgreementSnapshot`
- `AgreementSignProcess`
- `SignatureProcess`

### Value Objects
- `AgreementNo`
- `SnapshotVersion`
- `ChargeableAreaSnapshot`
- `ParkingProfileSnapshot`
- `MeterBindingSnapshot`
- `SignSagaRequestId`

## 5. Commands
Implement at minimum:
- `CreateAgreementCommand` (from accepted reservation; resource list comes from reservation, never client re-entry)
- `SubmitAgreementCommand` (DRAFT -> PENDING_APPROVAL; starts agreement-approval workflow)
- `ApproveAgreementCommand` (workflow callback; revalidates invariants)
- `StartSignatureCommand` (APPROVED -> WAITING_SIGNATURE)
- `SignAgreementCommand` (drives Sign Saga, see §8)
- `ActivateAgreementCommand` (SIGNED -> EFFECTIVE; job + command guard)
- `RequestTerminationCommand` (EFFECTIVE/EXPIRING -> TERMINATING; fee/penalty workflow)
- `CloseAgreementCommand` (EXPIRED/TERMINATED -> CLOSED; closure gates, see §9)
- `CreateRenewalCommand` (new Agreement with previous_agreement_id)
- `CreateAgreementChangeCommand` (new effective record, never overwrite history)

## 6. Queries
- `Agreement360` (agreement + items + snapshots + changes + handover + sign process)
- `ListExpiringAgreements` (T-N windows)
- agreement list by customer/resource (tenant scoped)

## 7. Application Flow - Create & Submit
1. Validate TenantContext, customer ACTIVE, quotation version ACCEPTED and reservation CONFIRMED (same tenant).
2. Source items from `reservation_item` via internal reservation API (ADR-023: alop-reservation is a separate service); never trust client-supplied resource list.
3. Freeze signing snapshot into `agreement_snapshot` (chargeableAreaSnapshot per item, PM fee rate/cycle, utility meter binding ids, utility tariff policy ids, parking profile snapshot, deposit terms).
4. Local transaction: insert `agreement` (DRAFT) + `agreement_item` + first snapshot; audit; COMMIT.
5. `SubmitAgreementCommand`: agreement FOR UPDATE, DRAFT -> PENDING_APPROVAL, start `agreement-approval` Flowable instance (TASK-011), persist `workflow_instance_id`.

## 8. Application Flow - Sign Saga (ADR-006)
1. `SignAgreementCommand` with Idempotency-Key; persist `agreement_sign_process` (request_id unique per tenant) in state PREPARING.
2. Local: agreement FOR UPDATE, WAITING_SIGNATURE, signature file hash recorded (`signature_process.file_sha256`).
3. Remote step: call alop-reservation `POST /internal/v1/reservations/{reservationId}/commit` with saga requestId; retry with backoff; UNKNOWN -> query/retry, never blind duplicate commit.
4. On COMMITTED: create occupancy expectation acknowledgement, transition agreement -> SIGNED, sign_process -> COMPLETED, Outbox `agreement.agreement.signed.v1` (consumers: billing/crm/notification).
5. If Asset committed but Agreement local commit fails: process -> COMPENSATING; invoke idempotent `ReleaseCommittedReservation`; physical occupancy delete forbidden.
6. Saga state machine and recovery job persist in `agreement_sign_process` (process_status/asset_commit_status/agreement_commit_status/retry_count); no in-memory-only saga.
7. `SIGNED != EFFECTIVE`: `AgreementEffectiveJob` transitions SIGNED -> EFFECTIVE only when `start_time <= now` (future lease signable today, LEASE-002).

## 9. Closure Gates (EXPIRED != CLOSED)
Agreement reaches EXPIRED/TERMINATED by time or termination, but CLOSED requires ALL of (`examples/agreement-close-checklist.md`, MASTER-SPEC §12):
1. No pending AgreementChange.
2. MOVE_OUT/Handover completed (when required); utility final settlement done (unless tenant policy allows fixed fee).
3. All related Occupancy ended/released.
4. All Receivable outstanding = 0 or approved WRITTEN_OFF; property fee billed to termination date; parking Occupancy/VehicleBinding ended.
5. Security deposit fully refunded/allocated/legally retained with records.
6. No payment/refund UNKNOWN or PROCESSING; no invoice/red-flush UNKNOWN/PROCESSING; quota final.
7. No unresolved CRITICAL reconciliation exception; no active sign/compensation Saga.
8. `CloseAgreementCommand` evaluates gates in one read-consistent pass; failure returns stable domain error listing unmet gates; never partial close.

## 10. Renewal Priority
- Default T-90 (tenant configurable 30/60/90/120/180): create RenewalPriority + CRM Renewal Opportunity + CRM Task + Notification trigger.
- STRICT: other customers cannot reserve; SOFT: viewing/quotation allowed but no final sign; NONE: no priority.
- `AgreementReminderJob` emits idempotent `agreement.agreement.expiring.v1` with `triggerKey=AGR:{agreementId}:D90` (also D60/D30/D15/D7/D1).

## 11. Database Deliverables
Flyway for `03-database/flyway/agreement/` (V1 baseline; extend if needed). Generate MyBatis DO/Mapper/Repository for:
- `agreement`, `agreement_item`, `agreement_snapshot`, `agreement_change`
- `agreement_sign_process`, `signature_process`
- `renewal_priority`, `handover_order`, `handover_item`

## 12. Idempotency
- Create: Idempotency-Key + agreementNo unique.
- Sign: Idempotency-Key + sign process request_id unique; CommitReservation saga requestId.
- Terminate/Close: Idempotency-Key + explicit request number; terminal states reject duplicates idempotently.
- Reminder job: tenantId + agreementId + triggerKey dedup.

## 13. Events (Outbox)
Produce exactly (schema files under `05-events/schemas/`):
- `agreement.agreement.signed.v1`
- `agreement.agreement.effective.v1`
- `agreement.agreement.expiring.v1`
- `agreement.agreement.expired.v1` (registry.md naming; local expiry fact)
Internal-only until registered: AgreementApproved / AgreementTerminated / AgreementClosed / renewal-priority.created.

## 14. API Surface (from API-CATALOG)
- `POST /api/admin/v1/agreements` -> `postAgreements`
- `POST /api/admin/v1/agreements/{id}/submit` -> `postAgreementsAgreementIdSubmit`
- `POST /api/admin/v1/agreements/{id}/sign` -> `postAgreementsAgreementIdSign`
- `POST /api/admin/v1/agreements/{id}/terminate` -> `postAgreementsAgreementIdTerminate`
- `POST /api/admin/v1/agreements/{id}/close` -> `postAgreementsAgreementIdClose`

## 15. Permissions
- `agreement:create`, `agreement:approve`, `agreement:sign`, `agreement:terminate`, `agreement:close`, `agreement:view`
- Close/terminate require finance-visible audit reason.

## 16. Required Metrics
- state transition counts; sign saga PREPARING/COMPENSATING backlog and age
- closure gate rejection reasons
- renewal priority conversion rate; reminder trigger volumes

## 17. Tests - Must Pass
### Domain (no Spring)
- Agreement state matrix incl. invalid transitions (e.g., DRAFT -> SIGNED rejected).
- Snapshot immutability: any mutation attempt on signed snapshot fails.
- SIGNED != EFFECTIVE enforcement; future lease stays SIGNED until start_time.
- Change creates new effective record; history untouched.

### Saga / Integration (Testcontainers MySQL/RabbitMQ)
- Sign happy path: agreement SIGNED + reservation CONVERTED + occupancy created.
- Asset commit succeeds, agreement local commit fails -> COMPENSATING -> ReleaseCommittedReservation called exactly once; retry recovery converges.
- Commit UNKNOWN -> query-before-retry; no duplicate occupancy.
- RabbitMQ down: local commits + Outbox pending; consumers catch up.

### Closure
- EXPIRED agreement with open receivable cannot CLOSE (stable error enumerating gates).
- All gates satisfied -> CLOSE succeeds exactly once; duplicate close idempotent.
- Handover incomplete blocks close (MOVEOUT-001 scenario).

### Tenant Isolation
- Tenant A cannot read/submit/sign/close Tenant B agreements; forged tenant header rejected.

### Idempotency
- Duplicate sign request x10 -> one saga, one SIGNED transition.
- Duplicate `agreement.agreement.expiring.v1` triggerKey -> one notification fact.

### Coverage Targets
- Domain >= 90% line/branch on invariants and state transitions.
- Application/integration >= 80% targeted orchestration and error paths.

## 18. Forbidden Implementation
- `contract.room_id` single-resource model; direct UPDATE of signed core fields.
- Treating Flowable completion as proof of business validity (must revalidate invariants).
- In-memory-only Sign Saga; deleting Occupancy rows as compensation.
- Generic update API that sets agreement status; reopening terminal states.
- Client-supplied resource list on create.
- Treating EXPIRED as CLOSED.

## 19. Definition of Done
- Compile; Flyway applies cleanly; OpenAPI contract tests pass.
- Event schemas validate; Outbox/Inbox flows verified.
- Domain >= 90%, application/integration >= 80% coverage.
- Sign saga success/compensation/recovery tests pass (E2E with alop-reservation stub per contract).
- Closure gate tests pass; renewal reminder tests pass.
- Tenant isolation, permissions, audit, metrics wired.
- No TODO/placeholder code.

## 20. SPEC Mapping
| Requirement | SPEC Source |
|---|---|
| Multi-item agreement, no room_id | MASTER-SPEC-V7.0 §6.2 |
| Snapshot immutable | 02-domain/agreement/DOMAIN-SPEC.md §8 |
| SIGNED != EFFECTIVE | MASTER-SPEC §6.2; state-machines.yaml (Agreement) |
| Sign Saga states/compensation | ADR-006; MASTER-SPEC §11 |
| Closure conditions | MASTER-SPEC §12; ADR-010; examples/agreement-close-checklist.md |
| Renewal T-90 default + STRICT/SOFT/NONE | MASTER-SPEC §8 |
| agreement-approval workflow | 06-workflow/agreement-approval.bpmn20.xml; workflow-spec.md |
| Signing snapshot fields (area/PM fee/meter/parking) | 02-domain/agreement/DOMAIN-SPEC.md §16 |
| Expiring reminders | 02-domain/agreement/DOMAIN-SPEC.md (V6.4 ownership); JOB-MATRIX (AgreementReminderJob) |
| E2E lease scenarios | 08-tests/e2e-scenarios.md LEASE-001/002, MOVEOUT-001; 12-test-data/e2e-lease-001.json |
