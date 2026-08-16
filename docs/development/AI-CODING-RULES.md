# AI Coding Rules

每个 Task 生成代码前：

1. 读 MASTER-SPEC-V7.0
2. 读对应 Domain SPEC
3. 读 DDL / OpenAPI / Event / State Machine
4. 输出 SPEC Implementation Mapping
5. 再写代码

禁止 AI：
- 自行新增 Bounded Context
- 合并 Bill 与 Receivable
- 把 Payment success 直接改 Bill paid
- 用 Redis 作为房源排期真相
- 直接 UPDATE 已签合同快照
- Blind retry UNKNOWN money operation
- 跨服务直接写库
