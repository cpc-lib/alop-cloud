# ALOP V7.0 Implementation Entrypoint

正式开发请按下面的顺序读取：

1. `spec/VERSION`
2. `spec/alop-v7.0/START-HERE.md`
3. `spec/alop-v7.0/00-master/MASTER-SPEC-V7.0.md`
4. `spec/alop-v7.0/11-codegen/CODEGEN-CONTRACT.md`
5. `spec/alop-v7.0/11-codegen/TASK-DEPENDENCY-GRAPH.md`
6. `spec/alop-v7.0/11-codegen/TASK-CONTEXT-MATRIX.yaml`
7. 目标 `spec/alop-v7.0/tasks/TASK-xxx.md`
8. 对应 `spec/alop-v7.0/14-task-bundles/TASK-xxx/CONTEXT.md`

每个 Task 还必须读取 Context Bundle 指向的：
- Domain SPEC / State Machine
- DDL / Data Dictionary
- OpenAPI
- Event Schema
- Transaction / Lock Matrix
- Idempotency Matrix
- Permission / Error Registry
- Test / Acceptance contracts

如果实现要求突破冻结的 Aggregate、Service Boundary、Finance Truth 或 Resource Inventory Invariant：
必须输出 `SPEC-GAP` + ADR，而不是由 AI 自行修改架构。
