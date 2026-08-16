# yudao-cloud Adoption

参考仓库：

`https://github.com/YunaiV/yudao-cloud`

## 采用策略

| yudao-cloud | ALOP | 策略 |
|---|---|---|
| yudao-dependencies | alop-dependencies | KEEP-CONCEPT |
| yudao-framework | alop-framework | KEEP + REWRITE |
| yudao-gateway | alop-gateway | KEEP-CONCEPT |
| yudao-module-system | alop-iam | REFACTOR |
| yudao-module-infra | alop-infra | REFACTOR |
| yudao-module-bpm | alop-workflow | REFACTOR |
| yudao-module-pay | alop-payment | PROVIDER REFERENCE ONLY |
| yudao-module-crm | alop-crm | UI/COMMON CRM REFERENCE ONLY |
| yudao-ui | alop-admin-web | DROP, REWRITE WITH REACT |
| mall/erp/wms/mes/iot/ai/member/im | - | DROP FROM ALOP CORE |

## 重要差异

- ALOP TenantContext 来自认证 Membership，拒绝信任任意 X-Tenant-Id。
- ALOP 跨服务一致性默认不用 Seata AT。
- Flowable 只编排审批。
- yudao-pay 只参考 Provider 接入，ALOP Payment Aggregate 自己实现。
