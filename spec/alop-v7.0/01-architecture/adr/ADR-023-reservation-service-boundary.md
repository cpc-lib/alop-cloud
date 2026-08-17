# ADR-023 Reservation Service Boundary

## Status
Accepted in V7.0.

## Context
Reservation（含 ResourceScheduleGuard、ResourceOccupancy、ResourceAvailability）的归属在 V6.x 历史中存在四方矛盾：

1. **DOMAIN-SPEC 与 02-domain 目录**：`02-domain/reservation/DOMAIN-SPEC.md` 已独立列出 5 张自有表（`reservation`、`reservation_item`、`resource_schedule_guard`、`resource_occupancy`、`resource_availability`），与 `02-domain/asset/` 并列。
2. **MASTER-SPEC §5 旧版服务边界**：仍把 Reservation/Occupancy 列在 `alop-asset` 服务下。
3. **代码库实际结构**：仓库根目录已存在独立的 `alop-reservation` 微服务模块（pom 与源码已建），并非 alop-asset 子包。
4. **V7.0 冻结清单**：第 1 节 Frozen bounded contexts 中 `Asset / Resource Inventory` 与 `Reservation` 是并列的两条冻结上下文。

四方事实不统一会让 AI Codegen 在 TASK-007（Reservation）等任务里无法判断“写哪库、归属哪服务、本地事务边界在哪”，进而误把 schedule_guard 写到 asset 库或反过来跨服务写表，破坏 ADR-002 的库存真相边界。

## Decision
1. **独立服务 alop-reservation**：与 `alop-asset` 平级，是 V7.0 一等公民微服务，不作为 asset 子模块。
2. **独立库 alop_reservation**：Flyway 迁移目录 `03-database/flyway/reservation/`。
3. **5 张表归 alop_reservation**：`reservation`、`reservation_item`、`resource_schedule_guard`、`resource_occupancy`、`resource_availability`。`resource_schedule_guard` 作为库存锁串行化点，必须随 Reservation/Occupancy/Availability 落在 reservation 本地库，以保持 ADR-002 要求的“修改排期 → 锁 schedule_guard → 同事务提交”这一本地事务闭环。
4. **resource_conflict_group 留在 alop-asset**：`resource_conflict_group` 与 `resource_conflict_group_member` 表继续由 asset 服务维护，作为资源互斥规则的权威真相；reservation 不直接读写 asset 库。
5. **ConflictGroup 本地投影 + 事件订阅**：alop-reservation 订阅 `asset.conflict-group.*` 事件（创建/成员变更/解散/状态切换），在本服务库维护只读投影表（或缓存）用于冲突预检；最终冲突裁决通过调用 asset 提供的内部 API 或提交 conflict 检查事件完成，不在本地投影上做权威判断。
6. **MASTER-SPEC §5 与 SERVICE-MODULE-MATRIX 同步**：服务边界清单补 alop-reservation，alop-asset 条目删除 Reservation/Occupancy。

## Consequences
- 库存锁 `resource_schedule_guard` 留在 reservation 本地库，Reservation/Occupancy/Availability 的排期变更保持单库本地事务，不再需要为加锁跨服务调用 asset。
- conflict_group 检查走本地只读投影 + asset 事件订阅；投影存在最终一致性窗口，因此严格冲突裁决（提交 Reservation 时）必须回查 asset 内部 API，不得只依赖投影。
- 多了一个独立服务，部署/治理成本略增；但换回库存真相边界完整、reservation domain 独立演进、AI Codegen 上下文一致。
- alop-asset 不再承担排期串行化职责，可专注资源主数据、评估、Offering/Listing、装修维修工单等业务。
- 备注：`operation_work_order` 表当前物理上仍位于 `alop_asset` 库（属 operations 服务域），按 V7.0 现状保留，不在本 ADR 中迁移；后续是否迁出由独立 ADR 决定。

## Alternatives Considered
- **A. 并入 alop-asset**：被否。理由：(a) 代码库已建独立 `alop-reservation` 微服务；(b) V7.0 冻结清单已把 Reservation 列为独立 bounded context；(c) `02-domain/reservation/` 目录独立存在；(d) 把 schedule_guard 留在 asset 会让 reservation 跨服务加锁，违反 ADR-002 本地事务要求。
- **B. 把 resource_conflict_group 一并迁到 reservation**：被否。ConflictGroup 描述的是资源本身的互斥规则（整租/分租、父子互斥），属资源主数据范畴，由 asset 维护更合理；且 asset 的 Offering/Listing/Renovation/Maintenance 都需要消费 ConflictGroup，迁出会造成新的反向耦合。

## Compliance
- `03-database/TABLE-CATALOG.yaml` 与 `03-database/flyway/asset/V1__init.sql`、`03-database/flyway/reservation/V1__init.sql` 必须保持一致：5 张迁出表只在 reservation 模块出现。
- `11-codegen/SERVICE-MODULE-MATRIX.yaml` 必须列出 alop-reservation（domainSpecs: [reservation]，owns: [Reservation, ScheduleGuard]，database: alop_reservation）。
- `docs/architecture/SERVICE-BOUNDARIES.md` 与 `00-master/MASTER-SPEC-V7.0.md` §5 服务边界必须同步包含 alop-reservation。
- `02-domain/reservation/DOMAIN-SPEC.md` §1 Bounded Context 必须为 `alop-reservation`，§9 Transaction/Locking 必须说明 schedule_guard 本地锁与 conflict_group 投影方案。
