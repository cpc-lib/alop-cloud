# UTILITY / PROPERTY MANAGEMENT FEE / PARKING DOMAIN SPEC

## 1. 定位与服务所有权
本 SPEC 不新增强制微服务，而是明确跨 Context 所有权：
- `alop-asset`：UtilityMeter、MeterBinding、MeterReading、ParkingSpaceProfile。
- `alop-crm`：CustomerVehicle。
- `alop-agreement`：ParkingVehicleBinding、合同中的停车位 AgreementItem、MoveIn/MoveOut 表计快照。
- `alop-billing`：UtilityTariffPlan、UtilityTariffTier、PROPERTY_MANAGEMENT_FEE / WATER / ELECTRICITY / PARKING_RENT 计费规则与 BillItem。
- `alop-finance/payment/invoice`：沿用既有应收、支付、核销、发票、账务与对账闭环。

## 2. 水电费业务闭环
`Meter Registration -> Bind Resource -> MoveIn Baseline -> Period Reading -> Submit -> Verify -> UsageVerified Event -> Billing -> Bill -> Receivable -> Payment/Collection/Allocation -> Invoice/Ledger -> MoveOut Final Reading -> Final Settlement`。

### 2.1 UtilityMeter
字段至少：meterNo、utilityType(WATER/ELECTRICITY)、unit(M3/KWH)、multiplier、readingMode(MANUAL/AUTO/IMPORT)、scope、status、manufacturer/serialNo、installedAt。

### 2.2 MeterBinding
支持表计绑定 Asset/Space/ResourceUnit；绑定必须有效期化。`DIRECT` 表示独享表；共享总表可使用 `AREA_SHARE/FIXED_RATIO/SUBMETER/MANUAL` 分摊。

### 2.3 MeterReading
读数是事实记录，不得在出账后覆盖。字段包括 periodStart/End、previousReading、currentReading、consumption、source、versionNo、supersedesReadingId、evidenceFileId、status、anomalyCode。

### 2.4 读数不变量
- 正常表：`currentReading >= previousReading`；负数必须是明确 `METER_REPLACED/ROLLOVER` 场景。
- `consumption=(current-previous)*multiplier`，人工调整必须独立 adjustment/correction trace。
- 同 meter + period 只能有一个 `VERIFIED current version`。
- `BILLED` 后禁止 UPDATE 数值；发现错误创建新 reading version，并生成差额 Bill/Receivable。
- MoveIn 基线和 MoveOut 最终读数必须关联 HandoverOrder。

### 2.5 表计异常
异常类型：`NEGATIVE_USAGE, ABNORMAL_SPIKE, MISSING_READING, METER_OFFLINE, DUPLICATE_PERIOD, MANUAL_OVERRIDE, METER_REPLACED`。异常读数不可自动出账，必须 `REVIEW_REQUIRED -> VERIFIED/REJECTED`。

### 2.6 共享表分摊
支持：
- `AREA_SHARE`：用量 × resourceChargeableArea / totalChargeableArea。
- `FIXED_RATIO`：租户预配置比例，总和必须 1.000000。
- `SUBMETER`：总表仅做校验，各分表按实际读数计费。
- `MANUAL`：必须原因、审批/权限与附件。
分摊结果总量不得超过源表计有效用量（允许显式配置公摊损耗项）。

## 3. 水电费率与计费
`UtilityTariffPlan` 归 Billing，支持 `FLAT, TIERED, TIME_OF_USE`。费率有 effectiveFrom/effectiveTo/version，不覆盖历史。

### 3.1 ChargeType
- `WATER`
- `ELECTRICITY`
- `EV_CHARGING_ELECTRICITY`
- `UTILITY_ADJUSTMENT`

### 3.2 计算
- FLAT：consumption × unitPrice。
- TIERED：按阶梯分段累计。
- TIME_OF_USE：峰/平/谷分别计算再求和。
- 计算全程 BigDecimal；BillItem 保存 readingId、tariffPlanId、tariffVersion、usage、unit、rate、calculationTrace。

## 4. 物业管理费闭环
`Agreement/Offering -> Property Fee Rule Snapshot -> BillingPlan -> Bill(PROPERTY_MANAGEMENT_FEE) -> Receivable -> Collection/Allocation -> Invoice/Ledger`。

### 4.1 计费方式
- `PER_AREA_PER_MONTH`：计费面积 × 元/㎡/月 × 期间。
- `FIXED_PERIOD`：每周期固定金额。
- `PER_RESOURCE_PERIOD`：每个资源每周期固定金额。
- `PERCENTAGE_OF_RENT`：租金费用 × 比例。

### 4.2 计费面积
必须使用签约时 `chargeableAreaSnapshot`，禁止以后 Resource.area 修改后回算历史账单。面积类型支持：`GROSS_AREA, NET_AREA, CHARGEABLE_AREA, CUSTOM`。

### 4.3 规则
- 支持 1~12 月账期。
- 支持免物业费期、折扣、阶梯费率、开始/结束日折算。
- 规则变更必须新版本 + effectiveFrom，不覆盖旧 BillingRule。
- 物业管理费与租金可以同一 Bill，也可以租户配置拆单；Finance 中最终仍为独立 Receivable/ChargeType 以便核销与开票追踪。

## 5. 车位租赁闭环
`Parking Resource -> Parking Offering -> Listing -> CRM/Quotation -> Reservation -> AgreementItem -> Parking Occupancy -> Parking Rent Bill -> Receivable -> Payment -> Collection/Allocation -> Invoice/Ledger -> Release/Change Vehicle`。

### 5.1 ParkingSpaceProfile
字段：parkingNo、zone、floor、parkingType(`STANDARD/MECHANICAL/EV_CHARGING/ACCESSIBLE`)、indoor、chargingSupported、chargerMeterId、vehicleSizeLimit、status。

### 5.2 出租模式
- 独立车位合同：Agreement 只包含 PARKING_SPACE Item。
- 组合合同：住宅/办公室 + 多个停车位属于同一 Agreement，多项独立价格/计费规则。
- 固定专属车位默认排他占用，必须复用 ScheduleGuard；任何重叠 Reservation/Occupancy 均拒绝。
- 可提前出租未来非重叠档期。

### 5.3 车辆档案与绑定
CustomerVehicle：plateNoCiphertext/plateNoHash、vehicleType、brand、model、color、status。车牌检索使用 HMAC hash，不允许全局跨租户检索。
ParkingVehicleBinding：agreementId/agreementItemId/parkingResourceId/vehicleId/effectiveFrom/effectiveTo/status。更换车辆不修改历史绑定，结束旧 binding 创建新 binding。

### 5.4 Parking ChargeType
- `PARKING_RENT`
- `PARKING_MANAGEMENT_FEE`
- `EV_CHARGING_ELECTRICITY`
- `PARKING_PENALTY`（如企业启用）

## 6. 交接与最终结算
MOVE_IN：记录 WATER/ELECTRICITY 表底、车位/车辆绑定、附件/照片。
MOVE_OUT：必须读取最终表数，生成未出账水电费；生成物业费/车位租金截止日折算；完成欠费、保证金抵扣、退款后才能 Agreement CLOSED。

## 7. Commands
- RegisterUtilityMeter
- BindUtilityMeter
- SubmitMeterReading
- VerifyMeterReading
- CorrectMeterReading
- ReplaceMeter
- CreateUtilityTariffPlan
- CreatePropertyManagementFeeRule
- CreateParkingSpaceProfile
- BindVehicleToParkingSpace
- ChangeParkingVehicle
- GenerateMoveOutUtilitySettlement

## 8. Queries
- GetMeterDetail
- GetMeterReadings
- GetUtilityUsage
- PreviewUtilityCharge
- PreviewPropertyManagementFee
- GetParkingAvailability
- GetParkingBindings

## 9. Events
- `asset.utility-meter.reading-verified.v1`
- `asset.utility-meter.reading-corrected.v1`
- `asset.parking.vehicle-bound.v1`
- `billing.utility-charge.billed.v1`
- `billing.property-fee.billed.v1`

## 10. 权限
- `utility:meter:view`
- `utility:meter:manage`
- `utility:reading:submit`
- `utility:reading:verify`
- `utility:reading:correct`
- `billing:utility-tariff:manage`
- `billing:property-fee:manage`
- `parking:view`
- `parking:manage`
- `parking:vehicle:bind`

## 11. 审计
必须审计：换表、人工读数、更正读数、共享表人工分摊、费率修改、物业费率修改、车牌绑定变更、手工费用调整。

## 12. Mandatory Tests
- Tenant A/B meter/parking isolation.
- Duplicate meter period and concurrent verify.
- Negative usage rejected unless replacement/rollover.
- Shared meter allocation totals.
- Tiered tariff boundary and rounding.
- Property fee area snapshot unaffected by later resource area changes.
- Parking concurrent reservation only one success.
- Parking can coexist with room in same Agreement.
- MoveOut final utility charge generated before Agreement CLOSED.
- Billed reading correction creates adjustment rather than mutating historical bill.
