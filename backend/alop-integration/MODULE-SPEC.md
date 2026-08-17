# alop-integration

## Authoritative Business Source

- `spec/alop-v7.0/00-master/MASTER-SPEC-V7.0.md`
- `spec/alop-v7.0/02-domain/platform-integration`
- `spec/alop-v7.0/09-operations`
- `spec/alop-v7.0/11-codegen/IDEMPOTENCY-MATRIX.yaml`
- `spec/alop-v7.0/11-codegen/TRANSACTION-LOCK-MATRIX.yaml`
- `spec/alop-v7.0/11-codegen/state-machines.yaml`
- corresponding `tasks/TASK-xxx.md`
- corresponding `14-task-bundles/TASK-xxx/CONTEXT.md`

## Implementation Rule

`MODULE-SPEC.md` is only an index. Business rules live in the central V7.0 SPEC.
Do not copy the full SPEC into the service.

Cross-service collaboration must use explicit API/event contracts; never write another service's database directly.
