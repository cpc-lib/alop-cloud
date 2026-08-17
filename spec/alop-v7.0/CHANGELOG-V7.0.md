# CHANGELOG V7.0

## Nature of this release
Frozen Codegen Baseline. No intentional new bounded context was introduced.

## Added
- V7 freeze policy and canonical truth boundaries.
- machine-readable service/module matrix.
- machine-readable state machines.
- transaction/lock, idempotency and job matrices.
- event producer/consumer registry.
- generated database table catalog/data dictionary.
- OpenAPI operation catalog and hardening of thin enterprise APIs.
- task dependency/context matrix and all TASK references normalized to V7.0.
- traceability matrix from business requirement -> tables/API/events/tests/tasks.
- canonical test fixtures.
- release gates/business closure/module DoD.
- automated codegen gap audit.

## Hardened (this round)
- **validate_spec.py**: enhanced from structural-only to 15-check structural + semantic validator (path param bidirectional, ASCII whitelist, money type enforcement, DDL precision assertion, event-registry cross-check, matrix operationId resolution, state-machine catalog, derived event matrix, task-bundle ref, codegen controls, V7 master ref, duplicate table, event envelope).
- **glm-5.3_common placeholder pollution**: cleaned 6 spec files + 5 miniapp API files.
- **State machines**: declared `state-machines.yaml` as canonical; fixed 7 conflict groups (PaymentOrder late success, RefundOrder, DunningCase, etc.); aligned domain STATE-MACHINE.md files.
- **Event registry three-way drift**: added 15+ missing events to `event-registry.yaml`; synchronized `registry.md`; `EVENT-PRODUCER-CONSUMER-MATRIX.yaml` now derived via `generate_event_matrix.py`.
- **Reservation service boundary (ADR-023)**: decided independent `alop-reservation` service with own database; synchronized 4 service-list artifacts (MASTER-SPEC §5, SERVICE-MODULE-MATRIX, SERVICE-BOUNDARIES, reservation DOMAIN-SPEC §1/§9).
- **DDL precision unification**: 27 fields corrected across 7 flyway modules — money `DECIMAL(18,2)`, quantities `DECIMAL(20,6)`, rates `DECIMAL(20,8)`; `DDL-CONTRACT.md` gained Money & Rounding Policy (HALF_UP, half-open intervals, optimistic lock + audit fields, tenant_id sentinel discipline).
- **generate_manifest.py parser rewrite**: fixed single-line multi-column bug (old parser took only the first column per line); catalog grew from partial to 141 tables / 1638 columns / 374 indexes.
- **OpenAPI money types**: all money fields changed from number/double to string+pattern; new `reservation.yaml` (8 endpoints), `iam.yaml` (16 endpoints), `app-portal.yaml` (24 endpoints); operation count 99 → 151.
- **TRANSACTION-LOCK-MATRIX**: all 16 operationId values aligned to actual OpenAPI operationIds.
- **Domain specs strengthened**: `tax` (34→149 lines), `ap` (45→159 lines), `owner-settlement` (41→150 lines) rewritten to 15-section codegen depth with aggregates, invariants, commands, tests; `finance` ChargeType enum added `PARKING_PENALTY`.

## Validation
- `validate_spec.py`: **PASS** — 0 errors, 0 warnings (151 API ops / 141 DDL tables / 41 event schemas / 33 tasks).
- `generate_manifest.py`: **PASS** — 141 tables / 1638 columns / 374 indexes.

## Compatibility
V7.0 is based on V6.5 domain scope. DDL/API/Event evolution after this freeze must follow explicit versioning and ADR rules.
