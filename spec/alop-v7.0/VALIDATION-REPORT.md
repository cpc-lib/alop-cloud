# ALOP-SaaS V7.0 Validation Report

Status: **PASS**

## Structural results
- OpenAPI operations: 151
- Unique OpenAPI operationIds: 151
- DDL CREATE TABLE definitions: 141
- DDL columns (catalog): 1638
- DDL indexes (catalog): 374
- Event JSON Schemas: 41
- Codegen TASKs: 33
- TASK context bundles: 33

## Checks performed
1. YAML / JSON / XML parseability.
2. OpenAPI 3.0.3 marker + operationId uniqueness.
3. Path parameter bidirectional check (placeholder <-> declared, required).
4. Path segment ASCII whitelist (homoglyph pollution detection).
5. Money field type enforcement (no number/double/float for money; string+pattern required).
6. DDL precision assertion (money 18,2 / quantity 20,6 / rate 20,8).
7. Event registry <-> lock/idempotency matrix successEvent cross-check.
8. Idempotency/lock matrix operationId exists in OpenAPI.
9. State machines registered in STATE-MACHINE-CATALOG.
10. EVENT-PRODUCER-CONSUMER-MATRIX derives from event-registry.
11. Task bundle CONTEXT.md referenced paths exist.
12. Required codegen controls present.
13. Task references MASTER-SPEC-V7.0 (not V6).
14. DDL duplicate table in same module.
15. Event schema envelope completeness (eventType + eventVersion).

## Reproducible command
```bash
python spec/alop-v7.0/scripts/validate_spec.py
python spec/alop-v7.0/scripts/generate_manifest.py
```

This is an offline structural + semantic codegen-contract validation. Provider sandbox tests, real MySQL Flyway execution and Java compilation belong to the implementation CI pipeline defined by each TASK/DoD.
