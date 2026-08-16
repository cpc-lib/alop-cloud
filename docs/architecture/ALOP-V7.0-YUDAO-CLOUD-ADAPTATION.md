# ALOP-SaaS V7.0 × yudao-cloud 底层技术架构目录

> 业务基线：房源管理租赁系统 `MASTER-SPEC-V7.0`
> 技术底座参考：`YunaiV/yudao-cloud`
> 原则：**yudao-cloud 提供通用平台能力，ALOP V7.0 SPEC 决定业务边界和业务真相。**

## 1. 总体目录

```text
alop-cloud/
├── pom.xml
├── README.md
├── SPEC-ENTRYPOINT.md
├── spec/
│   └── alop-v7.0/
├── docs/
│   ├── architecture/
│   ├── development/
│   ├── project-management/
│   └── operations/
├── alop-dependencies/
├── alop-framework/
│   ├── alop-common/
│   ├── alop-spring-boot-starter-web/
│   ├── alop-spring-boot-starter-security/
│   ├── alop-spring-boot-starter-tenant/
│   ├── alop-spring-boot-starter-datascope/
│   ├── alop-spring-boot-starter-mybatis/
│   ├── alop-spring-boot-starter-redis/
│   ├── alop-spring-boot-starter-lock/
│   ├── alop-spring-boot-starter-mq/
│   ├── alop-spring-boot-starter-idempotency/
│   ├── alop-spring-boot-starter-outbox/
│   ├── alop-spring-boot-starter-inbox/
│   ├── alop-spring-boot-starter-audit/
│   ├── alop-spring-boot-starter-job/
│   ├── alop-spring-boot-starter-observability/
│   ├── alop-spring-boot-starter-file-client/
│   ├── alop-spring-boot-starter-integration/
│   └── alop-test-support/
├── alop-gateway/
├── alop-tenant/
├── alop-iam/
├── alop-organization/
├── alop-infra/
├── alop-workflow/
├── alop-asset/
├── alop-reservation/
├── alop-crm/
├── alop-agreement/
├── alop-billing/
├── alop-tax/
├── alop-payment/
├── alop-finance/
├── alop-invoice/
├── alop-ap/
├── alop-owner-settlement/
├── alop-notification/
├── alop-operations/
├── alop-search/
├── alop-file/
├── alop-integration/
├── alop-admin-web/
├── alop-miniapp/
├── deployment/
├── scripts/
└── tools/
```

## 2. yudao-cloud 的正确使用方式

```text
yudao-cloud
     │
     ├── Maven 工程组织
     ├── dependencies / framework
     ├── Gateway
     ├── IAM / RBAC / 多租户基础
     ├── Infra
     ├── Flowable
     ├── 支付 Provider 接入经验
     ├── Redis / Redisson
     ├── Nacos / Sentinel / XXL-JOB
     └── MinIO
            │
            ▼
      ALOP Technical Foundation
            │
            ▼
          V7.0 SPEC
            │
      决定全部业务真相
```

禁止直接让 yudao CRM、Pay、BPM、Mall 的表模型反向决定 ALOP 业务。

## 3. Maven 模块组织

每个独立业务服务沿用 yudao-cloud 的 `api + server` 结构：

```text
alop-xxx/
├── pom.xml
├── alop-xxx-api/
│   └── src/main/java/com/company/alop/xxx/api/
│       ├── dto/
│       ├── enums/
│       ├── event/
│       └── client/
└── alop-xxx-server/
    └── src/main/
        ├── java/
        └── resources/
```

`api` 只允许 DTO、公开枚举、Feign/API Contract、Published Event Contract；禁止放 DO、Mapper、Aggregate、RepositoryImpl。

## 4. Server 内部 DDD 目录

```text
alop-xxx-server/
└── src/main/java/com/company/alop/xxx/
    ├── interfaces/
    │   ├── rest/
    │   ├── internal/
    │   └── mq/
    ├── application/
    │   ├── command/
    │   ├── query/
    │   ├── service/
    │   ├── saga/
    │   └── assembler/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   ├── policy/
    │   ├── specification/
    │   └── event/
    └── infrastructure/
        ├── persistence/
        │   ├── entity/
        │   ├── mapper/
        │   ├── repository/
        │   └── converter/
        ├── client/
        ├── mq/
        ├── integration/
        └── config/
```

Domain 禁止依赖 MVC、MyBatis、Redis、RabbitMQ、Flowable、MinIO。

## 5. alop-dependencies

统一控制 Java 21、Spring Boot 3.5.x、Spring Cloud 2025.0.x、Spring Cloud Alibaba 2025.0.x、MyBatis-Plus、MySQL、Redis、Redisson、RabbitMQ、Elasticsearch/OpenSearch、MinIO、Flowable、XXL-JOB、Flyway、MapStruct、Micrometer、OpenTelemetry、Testcontainers 等依赖版本。

## 6. Tenant / IAM / Organization

### alop-tenant

```text
domain/
├── tenant/
├── plan/
├── quota/
├── feature/
├── config/
├── route/
├── provisioning/
├── migration/
└── retention/
```

TenantContext 必须来自已认证 `UserMembership`，客户端 `X-Tenant-Id` 不可信；缺失 TenantContext 必须 Fail Closed。

### alop-iam

由 `yudao-module-system` 重点改造：

```text
domain/
├── user/
├── membership/
├── role/
├── permission/
├── auth/
├── oauth/
├── session/
└── supportsession/
```

### alop-organization

```text
domain/
├── organizationunit/
├── managementteam/
├── membership/
├── resourceresponsibility/
└── acl/
```

用于组织树、房源管理团队、负责人、查看/编辑/审批范围。

## 7. alop-workflow

参考 `yudao-module-bpm`，但 Flowable 只是流程执行器：

```text
domain/
├── processdefinition/
├── businessprocesslink/
├── approvalpolicy/
└── taskprojection/

infrastructure/
└── flowable/
```

适用于房源录入、评估、Listing、Agreement、Termination、Refund、Invoice、AP、Owner Settlement 等审批。

**Flowable Runtime Table != Business Truth**。

## 8. alop-asset

```text
domain/
├── asset/
├── space/
├── resourceunit/
├── conflictgroup/
├── valuation/
├── offering/
├── listing/
├── availability/
├── occupancy/
├── parking/
└── meter/
```

支持住宅、房间、办公室、商铺、工位、会议室、仓库、车位等统一 ResourceUnit。

## 9. alop-reservation

```text
domain/
├── reservation/
├── scheduleguard/
├── availability/
├── conflict/
└── priority/
```

资源时间统一 `[start,end)`；最终正确性来自 MySQL `resource_schedule_guard`，Redis 锁只能作为优化。整租/分租通过 `ResourceConflictGroup` 互斥。

## 10. alop-crm

```text
domain/
├── lead/
├── customer/
├── opportunity/
├── viewing/
├── quotation/
├── activity/
├── task/
├── assignment/
└── lostreason/
```

生命周期：`Lead -> Assignment -> QUALIFIED -> Customer/Opportunity -> Matching -> Viewing -> Quotation -> Reservation -> CONTRACTING -> WON`。

## 11. alop-agreement

```text
domain/
├── agreement/
├── item/
├── party/
├── snapshot/
├── change/
├── renewal/
├── termination/
├── resourcetransfer/
├── handover/
├── signature/
└── saga/
```

一份 Agreement 可包含多个 AgreementItem；禁止 `contract.room_id` 单资源模型。

## 12. alop-billing

```text
domain/
├── billingrule/
├── billingplan/
├── bill/
├── billitem/
├── calculator/
├── utility/
├── tariff/
├── propertyfee/
└── parkingfee/
```

支持 1~12 月账期、租金、物业费、水电、停车、充电、折扣、免租期、阶梯费率、起止日折算。历史规则采用版本 + `effectiveFrom`。

## 13. alop-tax

```text
domain/
├── taxcategory/
├── taxrule/
└── taxsnapshot/
```

Billing/Invoice 固化 tax category、tax mode、rate、net、tax、gross，禁止用新税率回算历史。

## 14. alop-payment

`yudao-module-pay` 只作为 Provider Adapter 参考，ALOP 支付聚合重写：

```text
domain/
├── paymentorder/
├── paymentattempt/
├── paymenttransaction/
├── refund/
├── merchantconfig/
└── callback/

infrastructure/provider/
├── wechat/
├── alipay/
└── unionpay/
```

正式模型：

```text
PaymentOrder
↓
PaymentAttempt
↓
PaymentTransaction
```

UNKNOWN 为一等状态。

## 15. alop-finance

```text
domain/
├── receivable/
├── collection/
├── allocation/
├── allocationreversal/
├── customeradvance/
├── adjustment/
├── writeoff/
├── refundreservation/
├── securitydeposit/
├── unidentifiedcollection/
├── accounting/
├── reconciliation/
├── dunning/
└── promisetopay/
```

核心规则：`Bill != Receivable`，`Payment SUCCESS != Receivable SETTLED`。

正式链路：

```text
PaymentSucceeded
→ Collection
→ Allocation
→ Receivable
→ AccountingEntry
```

## 16. alop-invoice

```text
domain/
├── application/
├── quota/
├── invoice/
├── relation/
├── redflush/
├── reissue/
└── delivery/

infrastructure/provider/
└── nuonuo/
```

链路：`Eligible Allocation -> InvoiceQuotaReservation -> Application -> Invoice -> RedFlush -> Quota Restore -> Reissue`。

## 17. alop-ap

```text
domain/
├── supplier/
├── supplierinvoice/
├── payable/
├── paymentrequest/
├── approval/
├── payout/
└── payouttransaction/
```

AR 与 AP 严格分离。

## 18. alop-owner-settlement

```text
domain/
├── owner/
├── settlementrule/
├── eligibility/
├── settlementbatch/
├── statement/
└── adjustmentbatch/
```

链路：`Allocated Revenue -> SettlementRule -> OwnerSettlementBatch -> OwnerStatement -> AP Payable -> Payout`。

## 19. alop-notification

```text
domain/
├── template/
├── recipient/
├── preference/
├── message/
├── delivery/
├── dedup/
├── retry/
└── receipt/
```

Channel：`IN_APP / SMS / EMAIL`。业务服务只发布事件，不能直接调用 SMS/SMTP SDK。

## 20. alop-operations

```text
domain/
├── workorder/
├── maintenance/
├── renovation/
├── cleaning/
├── inspection/
├── handoveroperation/
├── security/
├── customerissue/
├── sla/
└── conflicttask/
```

装修/维修必须检查未来 Reservation/Occupancy；有冲突创建 ConflictTask，不能强行覆盖。

## 21. alop-search

ES/OpenSearch 只保存派生 Read Model：

```text
ResourceSearchDocument
ListingSearchDocument
CustomerSearchDocument
AgreementSearchDocument
```

同步链路：`MySQL TX -> Outbox -> RabbitMQ -> Consumer -> ES`，禁止 MySQL + ES 双写。

## 22. alop-file

MinIO 作为对象存储，File Service 管理：

```text
FileObject
FileMetadata
FileBinding
StorageQuota
VirusScanResult
```

业务表只保存 `fileId`，不散落 MinIO URL。

## 23. alop-integration

管理 Provider Credential Metadata、Secret Reference、IntegrationTask、Webhook Inbox、外部渠道健康状态、Circuit Breaker 与 Integration Audit。Secret 本体不能进入普通业务表。

## 24. 分布式一致性

V7.0 默认：

```text
Local MySQL TX
  ├── Business Data
  └── Outbox
        ↓
RabbitMQ
        ↓
Inbox
        ↓
Consumer Local TX
```

每个业务服务拥有自己的 `mq_outbox` / `mq_inbox`，不是中央 Outbox DB。

需要持久化 Saga 的典型场景：Agreement Sign、Resource Transfer、Tenant Provisioning、Tenant DB Migration、Invoice Quota、Refund、AP Payout、Owner Settlement。

## 25. Seata 原则

yudao-cloud 提供 Seata，但 ALOP V7.0 默认不使用 Seata AT 作为跨服务正确性方案。核心采用：

```text
Local TX + Outbox + Inbox + Saga
```

只有经过 ADR 证明适合的短事务场景才允许引入 Seata。

## 26. XXL-JOB

核心任务建议：

```text
ReservationExpireJob
AgreementEffectiveJob
AgreementExpiryReminderJob
RenewalPriorityJob
BillingGenerateJob
ReceivableOverdueJob
DunningEscalationJob
PaymentUnknownQueryJob
RefundUnknownQueryJob
InvoiceUnknownQueryJob
RedFlushUnknownQueryJob
PayoutUnknownQueryJob
ReconciliationImportJob
ReconciliationMatchJob
NotificationRetryJob
SearchRepairJob
TenantMigrationJob
IntegrationRepairJob
```

Job Handler 只能调用 Application Service。

## 27. React 管理后台

不采用 yudao Vue UI，按项目技术要求：

```text
alop-admin-web/
└── src/
    ├── api/
    ├── components/
    ├── layouts/
    ├── router/
    ├── stores/
    ├── hooks/
    ├── permissions/
    └── features/
        ├── tenant/
        ├── iam/
        ├── organization/
        ├── asset/
        ├── crm/
        ├── agreement/
        ├── billing/
        ├── payment/
        ├── finance/
        ├── invoice/
        ├── ap/
        ├── owner-settlement/
        ├── operations/
        └── reconciliation/
```

技术：React + TypeScript + Ant Design + Zustand + Axios + TailwindCSS。

## 28. UniApp

```text
alop-miniapp/
├── pages/
│   ├── login/
│   ├── home/
│   ├── resource/
│   ├── viewing/
│   ├── quotation/
│   ├── reservation/
│   ├── agreement/
│   ├── bill/
│   ├── payment/
│   ├── invoice/
│   ├── workorder/
│   └── mine/
├── components/
├── api/
├── store/
└── utils/
```

## 29. SPEC 目录

```text
spec/
└── alop-v7.0/
    ├── 00-master/
    ├── 01-architecture/
    ├── 02-domain/
    ├── 03-database/
    ├── 04-openapi/
    ├── 05-events/
    ├── 06-state-machines/
    ├── 07-registries/
    ├── 08-tests/
    ├── 09-operations/
    ├── 10-codegen/
    ├── 11-test-data/
    ├── 12-task-bundles/
    ├── 13-acceptance/
    └── tasks/
```

每个服务只保留 `MODULE-SPEC.md` 指向中央 SPEC，不复制整套 SPEC。

## 30. docs 目录

```text
docs/
├── architecture/
│   ├── SYSTEM-CONTEXT.md
│   ├── SERVICE-BOUNDARIES.md
│   ├── MODULE-DEPENDENCIES.md
│   ├── TENANT-ARCHITECTURE.md
│   ├── SECURITY-ARCHITECTURE.md
│   ├── DATA-ARCHITECTURE.md
│   ├── EVENT-ARCHITECTURE.md
│   ├── WORKFLOW-ARCHITECTURE.md
│   └── YUDAO-ADOPTION.md
├── development/
│   ├── CODING-STANDARD.md
│   ├── DDD-PACKAGE-GUIDE.md
│   ├── API-GUIDE.md
│   ├── DATABASE-GUIDE.md
│   └── TEST-GUIDE.md
├── project-management/
│   ├── MASTER-ROADMAP.md
│   ├── MILESTONES.md
│   ├── RELEASE-PLAN.md
│   └── PROGRESS.md
└── operations/
    ├── LOCAL-DEV.md
    ├── DEPLOYMENT.md
    ├── BACKUP-RESTORE.md
    └── INCIDENT-RUNBOOK.md
```

## 31. deployment

```text
deployment/
├── docker-compose/
│   ├── docker-compose.base.yml
│   ├── docker-compose.middleware.yml
│   └── docker-compose.observability.yml
├── mysql/
├── nacos/
├── rabbitmq/
├── redis/
├── elasticsearch/
├── minio/
├── xxl-job/
└── k8s/
```

## 32. yudao-cloud 适配矩阵

| yudao-cloud | ALOP | 策略 |
|---|---|---|
| yudao-dependencies | alop-dependencies | KEEP-CONCEPT |
| yudao-framework | alop-framework | KEEP + REWRITE |
| yudao-gateway | alop-gateway | KEEP-CONCEPT |
| yudao-module-system | alop-iam | REFACTOR |
| yudao-module-infra | alop-infra | REFACTOR |
| yudao-module-bpm | alop-workflow | REFACTOR |
| yudao-module-pay | alop-payment | PROVIDER-REFERENCE ONLY |
| yudao-module-crm | alop-crm | BUSINESS-REFERENCE ONLY |
| yudao-module-report | Read Model / React | OPTIONAL REFERENCE |
| yudao-module-member | - | DROP |
| yudao-module-mall | - | DROP |
| yudao-module-erp | - | DROP |
| yudao-module-wms | - | DROP |
| yudao-module-mes | - | DROP |
| yudao-module-iot | - | DROP NOW |
| yudao-module-ai | - | DROP NOW |
| yudao-module-im | - | DROP NOW |
| yudao-ui | alop-admin-web | DROP / React REWRITE |

## 33. 最终原则

```text
ALOP V7.0 SPEC
       │
       ▼
Business Module Boundary
       │
       ▼
DDD Application / Domain
       │
       ▼
Infrastructure Adapter
```

**yudao-cloud = Technical Foundation**  
**ALOP V7.0 = Business Source of Truth**
