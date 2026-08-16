# ADR-011 — 水电计量、物业管理费与车位租赁建模

## Status
Accepted — V6.3

## Context
水费、电费、物业管理费和车位租赁如果只作为 `BillItem.charge_type`，无法保证抄表来源、费率版本、计费面积、车位排期、车辆绑定、更正与退租结算可审计，也无法让 AI Coding 生成稳定的领域代码。

## Decision
1. **水电表计事实归 Asset/Operations**：`utility_meter`、`utility_meter_binding`、`utility_meter_reading` 归资源运营侧，记录物理表、绑定关系与不可覆盖的读数版本。
2. **水电费率与金额计算归 Billing**：`utility_tariff_plan/tier` 与 BillingRule 归 Billing；已核验用量通过事件进入 Billing，产生 BillItem；金额事实不在表计服务直接写入 Finance。
3. **物业管理费归 BillingRule**：使用 `PROPERTY_MANAGEMENT_FEE`，计费基数必须签约快照，支持 `PER_AREA_PER_MONTH / FIXED_PERIOD / PER_RESOURCE_PERIOD / PERCENTAGE_OF_RENT`。
4. **车位是 ResourceUnit**：`resource_type=PARKING_SPACE`，复用 Offering/Reservation/ScheduleGuard/Agreement/Occupancy/Billing/Finance；不得另建一套孤立“停车合同系统”。
5. **车辆档案归 CRM，车位-车辆履约绑定归 Agreement/Operations**：车牌可变更但必须保留生效历史。
6. **充电车位电费复用 UtilityMeter**：停车位可绑定 ELECTRICITY Meter，并以 `EV_CHARGING_ELECTRICITY` 生成独立费用。

## Consequences
- 水电用量、物业费、停车租金均可形成 Bill → Receivable → Payment → Collection → Allocation → Invoice → Ledger → Reconciliation 闭环。
- MoveIn/MoveOut 必须采集必要表计读数，否则水电最终结算不能 CLOSED（租户策略允许固定费时除外）。
- 已经出账的表计读数不得修改历史 Bill；更正通过新 Reading Version + Adjustment Bill/Receivable 实现。
