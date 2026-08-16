# Codegen Gap Audit — V7.0

## Automated findings after normalization

**PASS — no unresolved structural contract issue detected by the V7 offline validator.**

Validated conditions include:
- all OpenAPI YAML parses;
- all 99 API operations have globally unique `operationId` values;
- every `{pathParameter}` is explicitly declared and required;
- all task files reference `MASTER-SPEC-V7.0.md` and contain no stale V6 MASTER reference;
- all 27 event JSON Schemas parse and expose event type/version fields;
- 141 `CREATE TABLE` definitions have no duplicate table creation inside the same service schema module;
- canonical registries and codegen matrices parse;
- every TASK-001..033 has a `14-task-bundles/TASK-xxx/CONTEXT.md` file.

## Known implementation-time decisions intentionally not frozen
These are extension points, not SPEC gaps:
- exact SMS/email/cloud provider SKU: use existing provider SPI;
- exact e-sign vendor: use `SignatureProvider` SPI;
- jurisdiction-specific tax rates: configure effective-dated `TaxRule`, never hard-code into Billing;
- physical sharding activation threshold: decide from measured production load while preserving `tenant_id` as primary routing dimension;
- enterprise-specific accounting chart mapping: use effective tenant accounting configuration while preserving posting invariants.

## Freeze rule
Any future need to change bounded context ownership, financial truth, ScheduleGuard semantics, cross-tenant routing or breaking API/Event semantics is no longer an ordinary implementation choice. It requires an ADR and a versioned contract change.
