# ALOP-SaaS V7.0 — Start Here

This repository is the frozen implementation contract for Spring Cloud teams and AI Coding agents.

## 1. Validate the package
```bash
python scripts/validate_spec.py
```

## 2. Choose the next task
Read:
- `11-codegen/TASK-DEPENDENCY-GRAPH.md`
- `11-codegen/TASK-CONTEXT-MATRIX.yaml`

Start at `TASK-001` for a new implementation. Do not jump to a downstream task unless its dependencies and contracts already exist.

## 3. Load the task bundle
Example:
```text
14-task-bundles/TASK-015/CONTEXT.md
```
This file lists the exact SPEC/DDL/API/security/codegen contracts the coding agent must read.

## 4. Generate in one use-case slice
Prefer:
`CreateReservation` -> tests -> migration/API/event verification -> merge
instead of generating an entire microservice in one uncontrolled pass.

## 5. Required delivery from an AI Coding agent
- file tree;
- production Java implementation;
- migrations;
- OpenAPI/event changes only when permitted by baseline;
- unit/integration/tenant isolation/concurrency tests;
- metrics/audit;
- README;
- SPEC implementation mapping.

## 6. Stop conditions
If a requested implementation requires changing a frozen aggregate/service/financial truth/inventory invariant, output `SPEC-GAP` and create an ADR proposal. Do not silently invent architecture.
