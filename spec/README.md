# ALOP Specification

当前冻结业务基线：

`alop-v7.0`

完整 SPEC 已直接集成在：

`spec/alop-v7.0/`

## 开发入口

1. `alop-v7.0/START-HERE.md`
2. `alop-v7.0/00-master/MASTER-SPEC-V7.0.md`
3. `alop-v7.0/11-codegen/TASK-DEPENDENCY-GRAPH.md`
4. `alop-v7.0/11-codegen/TASK-CONTEXT-MATRIX.yaml`
5. 目标 `tasks/TASK-xxx.md`
6. `14-task-bundles/TASK-xxx/CONTEXT.md`

## 单一事实源

各 Java 服务的 `MODULE-SPEC.md` 只能引用中央 SPEC，禁止复制并维护第二套业务规则。
