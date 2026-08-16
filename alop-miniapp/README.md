# ALOP MiniApp — UniApp 用户端

这是 ALOP-SaaS 的租客/客户用户端工程目录。

覆盖 V7.0 用户侧核心闭环：

```text
登录
→ 首页/房源
→ 预约看房
→ 报价
→ Reservation
→ Agreement
→ 签署/交接
→ Bill
→ Payment
→ Invoice
→ WorkOrder
→ Notification
→ Mine
```

## 目录原则

- 页面只负责交互，不直接编写复杂业务规则。
- API 层按后端 bounded context 分类。
- 资金状态以服务端 Payment/Finance 事实为准。
- 客户端 SDK 支付 success 只能展示“确认中”，不得自行认定支付成功。
- TenantContext 由登录后的 membership/session 决定，客户端不能伪造租户。
