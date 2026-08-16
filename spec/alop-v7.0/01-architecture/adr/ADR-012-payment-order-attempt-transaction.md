# ADR-012 PaymentOrder / PaymentAttempt / PaymentTransaction

## Status
Accepted in V6.3.

## Context
真实在线支付存在：预支付创建、客户端拉起、用户取消、网络超时、渠道结果 UNKNOWN、重复回调、延迟回调、切换渠道、部分退款和对账补单。若只使用一张 `payment_order` + 一个 status，会把“业务支付意图”“一次渠道尝试”“真实资金交易”混为一体，导致无法准确处理 UNKNOWN 和重复支付风险。

## Decision
拆分：
- `PaymentOrder`：业务支付意图；
- `PaymentAttempt`：一次具体渠道尝试；
- `PaymentTransaction`：渠道确认的资金事实；
- `PaymentChannelRequest`：每次第三方调用审计；
- `RefundOrder/RefundTransaction`：退款独立事实。

`UNKNOWN` 主要存在于 Attempt / Refund 外部交互结果中。PaymentOrder Read Model 可以暴露 `processingState=UNKNOWN`，但不得把 UNKNOWN 当成可重试失败。

## Consequences
优点：
- 支持安全的渠道重试/切换；
- UNKNOWN 可以查询恢复；
- 重复 callback 可精确幂等；
- 晚到成功仍能保留资金真相；
- Finance 只消费稳定的 PaymentSucceeded/PaymentRefunded 事实。

代价：
- 表和状态机更多；
- Application 编排更复杂；
- 需要支付异常运营台。
