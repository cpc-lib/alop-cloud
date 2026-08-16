# ALOP-SaaS V7.0 Validation Report

Status: **PASS**

## Structural results
- OpenAPI operations: 99
- Unique OpenAPI operationIds: 99
- DDL CREATE TABLE definitions: 141
- Event JSON Schemas: 27
- Canonical state machines: 26
- Codegen TASKs: 33
- TASK context bundles: 33

## Checks performed
1. YAML parse for all `.yaml` files.
2. JSON parse for all `.json` files.
3. BPMN/XML parse for all `.xml` files.
4. OpenAPI 3.0.3 marker check.
5. OpenAPI operationId presence and global uniqueness.
6. OpenAPI path-parameter declaration/required check.
7. Event envelope eventType/eventVersion check.
8. V7 MASTER reference check for every TASK.
9. Stale V6 MASTER reference rejection for TASK files.
10. Duplicate CREATE TABLE detection inside each service schema module.
11. Required codegen-control file existence.
12. TASK context-bundle coverage.
13. ZIP CRC/integrity test during packaging.

## Reproducible command
```bash
python scripts/validate_spec.py
```

This is an offline structural/codegen-contract validation. Provider sandbox tests, real MySQL Flyway execution and Java compilation belong to the implementation CI pipeline defined by each TASK/DoD.
