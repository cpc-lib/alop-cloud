# Permission Model

Access = TenantMembership AND RBAC Permission AND DataScope AND ResourceACL AND optional FieldPermission.

DataScope: SELF, TEAM, DEPARTMENT, DEPARTMENT_TREE, COMPANY, REGION, ASSIGNED_ASSET, CUSTOM, ALL.
High-risk commands (refund, reverse allocation, finance adjustment, red flush, route update) require elevated risk level, reason, audit and optional MFA/Flowable approval.


## 水电/物业费/车位
- `utility:meter:view/manage`
- `utility:reading:submit/verify/correct`
- `billing:utility-tariff:manage`
- `billing:property-fee:manage`
- `parking:view/manage`
- `parking:vehicle:bind`
读数更正、共享表人工分摊、费率变更属于高风险操作，必须原因 + Audit；租户可配置审批。
