# E2E Closed-loop Scenarios

## LEASE-001 Normal lease
Tenant -> Asset approval -> Valuation -> Offering -> Listing -> Lead -> Customer -> Opportunity -> Viewing -> Quotation V1/V2 -> Reservation multiple resources -> Flowable agreement approval -> electronic signature -> Sign Saga -> Occupancy -> Billing -> Bill -> Receivable -> WeChat payment -> Collection -> Allocation -> Ledger -> Invoice -> T+1 reconciliation -> T-90 renewal -> old agreement CLOSED.

## LEASE-002 Future lease
Existing [2026-01-01,2027-01-01), new [2027-01-01,2028-01-01) succeeds.

## LEASE-003 Renovation conflict
Renovation overlaps requested lease => RESOURCE_TIME_CONFLICT.

## LEASE-004 Whole/room conflict
Room leased prevents whole-unit lease and vice versa.

## MOVEOUT-001 Early termination
Termination approval -> move-out -> damage receivable -> deposit offset -> refund -> occupancy release -> financial/invoice finalization -> Agreement CLOSED.

## PAY-001 Late provider success
Local UNKNOWN, provider later SUCCESS -> exactly one transaction/collection/ledger effect.

## INV-001 Red flush/reissue
Blue -> RedFlush -> quota restored -> new application -> new Blue with traceable relation.

## TENANT-001 Isolation
Tenant A cannot read/write B by forged business id/header/cache/ES/file/workflow routes.


## E2E-UTIL-001 水电抄表到收款
1. Tenant A 房间绑定 WATER/ELECTRICITY meter。
2. MOVE_IN 写入初始读数。
3. 月末提交读数并 VERIFIED。
4. Billing 使用生效 Tariff 生成 WATER/ELECTRICITY BillItem。
5. Finance 生成 Receivable；支付、Collection、Allocation、Invoice、Ledger 全链路完成。
6. Tenant B 不得读取 A 的 meter/reading/tariff。

## E2E-UTIL-002 已出账读数更正
已 BILLED 的电表读数错误，不允许 UPDATE；创建 corrected version，Billing 生成差额 Adjustment，原 Bill/Receivable 保持审计可追溯。

## E2E-PROPERTY-001 物业管理费
签约计费面积 120.50㎡，单价 18.00 元/㎡/月，季度付。签约后 Resource.area 改为 125㎡，历史及已签规则仍使用 120.50㎡ snapshot；新规则只从 effectiveFrom 生效。

## E2E-PARK-001 房屋+车位组合出租
Agreement 同时包含 OFFICE 801 + PARKING P001/P002；三项各自 ScheduleGuard/Occupancy，车位单独 PARKING_RENT；换车仅结束旧 VehicleBinding 并创建新 binding，不修改历史。

## E2E-PARK-002 车位并发
100 个请求同时租 P001 同时间段，只有 1 个 Reservation 成功；Redis down 时仍不超卖。

## E2E-MOVEOUT-UTIL-001 退租最终水电结算
MOVE_OUT 采集最终水电读数，生成最终 Utility Bill/Receivable；在最终应收未结清/正式 write-off 前 Agreement CLOSED 必须失败。
