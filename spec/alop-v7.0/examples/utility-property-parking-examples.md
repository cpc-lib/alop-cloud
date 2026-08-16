# 水电 / 物业管理费 / 车位示例

## 水费
上期 120.000 m³，本期 132.500 m³，倍率1，用量12.500 m³；单价4.20元/m³：52.50元。BillItem trace 必须记录 readingId/tariffVersion。

## 物业管理费
计费面积 120.50㎡，18元/㎡/月，季度：`120.50 * 18 * 3 = 6507.00`。面积取签约 snapshot。

## 车位
Agreement AGR001：Office801 30,000/月 + Parking P001 800/月 + P002 800/月；同合同多 AgreementItem、多 BillingRule，P001/P002 各自排期。

## 退租
MOVE_OUT 最终电表比上期增加 168kWh；生成最终 Electricity Bill 后，才能继续保证金最终结算和 Agreement CLOSED 检查。
