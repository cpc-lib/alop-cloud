# ADR-007 Tenant Payment Merchant / Credential Isolation

## Status
Accepted and hardened in V6.3.

## Context
平台支持 `PLATFORM_MERCHANT` 与 `TENANT_MERCHANT`。不同租户可能分别使用自己的微信商户号、支付宝应用、银联商户配置。支付回调是公网入口，若仅信任 callback 参数中的 tenantId 或客户端 Header，可能造成跨租户资金归属错误。

## Decision
1. Payment Service 持有 `tenant_payment_merchant` 元数据。
2. 商户密钥、私钥、API Key、证书私钥只存 SecretManager，数据库保存 reference。
3. Merchant config 至少由 `tenantId + channel + merchantId + appId + effective period` 定位。
4. 创建 PaymentAttempt 时把 `merchantConfigId` 固化到 Attempt。
5. Provider Callback Tenant Resolution 依赖可信的渠道商户身份、全局唯一 `paymentNo`、`providerTradeNo`，不信任 tenant 参数。
6. 若 merchantId/appId 与 PaymentAttempt 固化配置不匹配，拒绝业务状态更新并触发安全指标。
7. Platform Support 也不能读取 Credential secret，只能看到 masked metadata/reference 状态。
8. Credential rotation 新增新版本/新 secret reference；历史支付保留 merchant config ID，不覆盖历史事实。

## P0 Conditions
- Tenant A payment 使用 Tenant B merchant credential；
- 回调被解析到错误 Tenant；
- 相同 provider trade number 出现在不同租户/不同订单；
- merchant route 被错误修改。

## Consequences
多租户支付隔离不依赖用户可伪造字段，而依赖渠道身份和服务端支付事实。
