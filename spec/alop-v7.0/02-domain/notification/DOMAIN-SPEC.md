# NOTIFICATION DOMAIN SPEC — SMS / EMAIL / IN-APP

## 1. Bounded Context / Service
`alop-notification`

通知中心是独立 Bounded Context。业务服务只能发布业务事件或创建明确的 NotificationRequest，不允许直接调用短信 SDK、SMTP、邮件厂商 API。

## 2. Business Goal
统一承接房源运营平台内所有提醒与业务通知：
- 合同到期/续租优先权提醒；
- 账单生成、到期、逾期催收；
- Reservation 即将到期/已确认；
- 支付成功、退款结果；
- 发票已开具、发票邮件发送/重发；
- 看房/CRM 跟进任务；
- 维修/工单 SLA；
- 平台运营异常与人工任务提醒。

渠道 V6.4 至少支持：
- `IN_APP`
- `SMS`
- `EMAIL`

预留：`WECHAT_MINI_PROGRAM / ENTERPRISE_WECHAT / WEBHOOK`。

## 3. Aggregate Roots
- `NotificationRule`：业务事件到通知动作的规则。
- `NotificationTemplate`：模板及版本。
- `NotificationMessage`：一次逻辑通知实例。
- `NotificationDelivery`：某条逻辑通知在某渠道/收件人上的投递事实。
- `RecipientPreference`：客户/员工的通知偏好与联系方式策略。
- `NotificationProviderConfig`：租户级渠道配置（只存 credentialRef）。

## 4. Owned Tables
- `notification_rule`
- `notification_template`
- `notification_message`
- `notification_delivery`
- `notification_delivery_attempt`
- `notification_recipient_preference`
- `notification_provider_config`
- `notification_suppression`

## 5. Commands
- `CreateNotificationRule`
- `UpdateNotificationRule`
- `EnableNotificationRule`
- `DisableNotificationRule`
- `CreateTemplateVersion`
- `RequestNotification`
- `RetryNotificationDelivery`
- `CancelPendingNotification`
- `HandleSmsDeliveryReceipt`
- `HandleEmailDeliveryReceipt`
- `SuppressRecipient`
- `UpdateRecipientPreference`

## 6. Queries
- `GetNotificationMessage`
- `ListNotificationDeliveries`
- `ListFailedDeliveries`
- `GetRecipientPreference`
- `PreviewNotificationTemplate`
- `GetNotificationRule`

## 7. Produced Events
- `NotificationQueued`
- `NotificationSent`
- `NotificationDelivered`
- `NotificationFailed`
- `NotificationBounced`
- `NotificationSuppressed`

## 8. Consumed Business Events
至少消费：
- `agreement.agreement.expiring.v1`
- `agreement.renewal-priority.created.v1`
- `billing.bill.issued.v1`
- `finance.receivable.overdue.v1`
- `payment.payment.succeeded.v1`
- `payment.refund.succeeded.v1`
- `invoice.invoice.delivery-requested.v1`
- `asset.reservation.expiring.v1`
- `operations.work-order.sla-violated.v1`

通知服务消费业务事件后根据 `NotificationRule` 决定是否产生消息；业务事件本身不得包含完整手机号/邮箱等 PII。

## 9. Notification Categories
- `MARKETING`：营销，可退订，必须尊重偏好与同意状态。
- `TRANSACTIONAL`：账单、支付、发票等交易通知。
- `LEGAL`：合同到期、违约、法务等重要通知。
- `OPERATIONAL`：内部员工、工单、异常提醒。

`MARKETING` 被退订后禁止继续发送；`TRANSACTIONAL/LEGAL` 不受营销退订影响，但必须有合法业务关系和可用联系方式。

## 10. Notification Rule
核心字段：
- `tenantId`
- `ruleCode`
- `businessEventType`
- `recipientStrategy`
- `channels[]`
- `templateCodeByChannel`
- `triggerOffset`
- `priority`
- `fallbackPolicy`
- `quietHourPolicy`
- `status`
- `version`

`recipientStrategy`：
- `CUSTOMER_PRIMARY_CONTACT`
- `CUSTOMER_ALL_BILLING_CONTACTS`
- `AGREEMENT_CONTACT`
- `ASSET_MANAGER`
- `OPPORTUNITY_OWNER`
- `FINANCE_OWNER`
- `WORK_ORDER_ASSIGNEE`
- `EXPLICIT_RECIPIENT`

## 11. Reminder Scheduling
两类来源：

### 11.1 Event-driven
例如 `BillIssued / PaymentSucceeded / InvoiceDeliveryRequested`，消费事件后立即生成 NotificationMessage。

### 11.2 Time-driven
例如合同 T-90/T-60/T-30、账单到期前 7 天、Reservation 到期前 15 分钟。
业务 Context 负责确定“业务触发点”并发布事件；Notification 不扫描 Agreement/Receivable 业务表。

例如 Agreement Service：
`AgreementReminderJob -> AgreementExpiringReminderTriggered(triggerKey=AGR123:D90) -> Notification Service`。

## 12. Default Reminder Matrix
Tenant 可覆盖，但推荐默认：

| Business | Trigger | Customer | Internal | Channel |
|---|---|---:|---:|---|
| Agreement expiry | T-90 | yes | manager | SMS+EMAIL+IN_APP |
| Agreement expiry | T-30 | yes | manager | SMS+EMAIL |
| Agreement expiry | T-7 | yes | manager | SMS+EMAIL |
| Renewal priority | created | yes | owner | SMS+EMAIL |
| Bill issued | immediate | yes | finance optional | EMAIL+IN_APP |
| Bill due | T-7 | yes | no | SMS+EMAIL |
| Bill due | T-1 | yes | no | SMS+EMAIL |
| Receivable overdue | D+1 | yes | owner | SMS+EMAIL |
| Receivable overdue | D+7 | yes | finance | SMS+EMAIL |
| Payment success | immediate | yes | no | SMS+EMAIL(optional) |
| Refund success | immediate | yes | finance optional | SMS+EMAIL |
| Invoice issued | immediate | yes | no | EMAIL |
| Reservation expiring | T-15min | yes | no | SMS+IN_APP |
| WorkOrder SLA | violated | no | manager | EMAIL+IN_APP |

## 13. Deduplication
逻辑通知 dedup key：
`tenantId + businessType + businessId + ruleCode + triggerKey + recipientRef`。

渠道投递 dedup key：
`notificationMessageId + channel + recipientAddressHash + templateVersion`。

重复 MQ、重复 Job、服务重启不得产生重复短信/邮件。

## 14. NotificationMessage State
`CREATED -> QUEUED -> PROCESSING -> COMPLETED`

如果所有渠道最终失败：`FAILED`。
如果取消且尚未投递：`CANCELLED`。

多渠道消息允许：SMS 成功、EMAIL 失败；此时 Message 可 `PARTIALLY_COMPLETED`。

## 15. NotificationDelivery State
`PENDING -> SENDING -> SENT -> DELIVERED`

异常：
- `RETRY_WAIT`
- `FAILED`
- `BOUNCED`（EMAIL）
- `REJECTED`（SMS/EMAIL provider）
- `SUPPRESSED`
- `CANCELLED`

`SENT` 表示 Provider 接受；只有 Provider 支持回执时才可进入 `DELIVERED`。SMTP 场景通常只能确认 accepted，不能伪造 delivered。

## 16. Provider Abstraction
```java
interface SmsProvider {
    SmsSendResult send(SmsSendRequest request);
    ProviderCapability capability();
}

interface EmailProvider {
    EmailSendResult send(EmailSendRequest request);
    ProviderCapability capability();
}
```

实现可为平台共享或 Tenant 自有配置。禁止 Domain 依赖具体厂商 SDK。

## 17. Tenant Provider Mode
- `PLATFORM_PROVIDER`
- `TENANT_PROVIDER`

数据库只保存：
- provider code
- sender identity
- credential reference
- callback configuration reference

Secret/API Key/SMTP password 必须在 SecretManager。

## 18. SMS Rules
- 手机号以密文保存，路由/去重使用 HMAC hash；
- 模板必须经过版本化；
- Provider 要求模板 ID 时，保存 providerTemplateRef；
- 发送前执行模板变量完整性校验；
- 对同一手机号实施 Tenant 级频率保护；
- 交易/法律短信与营销短信分别限流；
- Provider 返回 UNKNOWN 时不得立即重复发送，先查询/等待回执（若 Provider 支持）。

## 19. Email Rules
邮件支持：
- HTML + plain text fallback；
- `TO / CC / BCC`；
- Reply-To；
- 附件 FileId；
- 租户品牌主题、Logo、Footer；
- Tenant 自定义发件人（按 Provider 能力）；
- Bounce/Complaint 处理（Provider 支持时）。

邮箱地址不得进入日志明文；日志只显示脱敏地址和 hash。

## 20. Quiet Hours
Tenant 可配置普通提醒静默时段，例如 `22:00-08:00`。
- `MARKETING`：必须遵守；
- `TRANSACTIONAL`：默认遵守，可规则级 override；
- `LEGAL/CRITICAL_OPERATIONAL`：可明确配置 bypass。

所有时间按 Tenant timezone 计算。

## 21. Fallback Policy
支持：
- `NONE`
- `SMS_THEN_EMAIL`
- `EMAIL_THEN_SMS`
- `PARALLEL_SMS_EMAIL`

Fallback 只能在原渠道明确 `FAILED/REJECTED` 后执行；渠道为 `UNKNOWN` 时不得立刻切备用渠道，避免重复通知。

## 22. Retry Policy
推荐：
- 第一次失败：1 min
- 第二次：5 min
- 第三次：30 min
- 第四次：2 h
- 超过上限：`FAILED` + `IntegrationTask`

`INVALID_ADDRESS / UNSUBSCRIBED / TEMPLATE_INVALID` 为 non-retryable。
网络错误、Provider 5xx 为 retryable。

## 23. Security / Privacy
- Notification Event 不携带完整手机号/邮箱；
- Recipient Resolver 从 Customer/Agreement/Internal API 获取或使用 delivery instruction；
- 手机号/邮箱密文落库；
- address hash 用于 dedup/索引；
- 邮件正文和短信内容日志默认不落完整内容，仅保存 templateVersion + contentHash；
- SupportSession 访问投递详情必须审计。

## 24. Audit
必须记录：
- rule/template version；
- recipient strategy；
- channel；
- provider；
- business source；
- operator（手工重发时）；
- failure reason；
- retry；
- final delivery status。

## 25. Metrics
- `notification_created_total`
- `notification_delivery_sent_total{channel}`（channel 低基数）
- `notification_delivery_failed_total{channel,reason_class}`
- `notification_retry_backlog`
- `notification_email_bounce_total`
- `notification_sms_reject_total`
- `notification_invoice_email_success_total`
- `notification_invoice_email_failure_total`

禁止 tenantId 作为无限基数 Prometheus label。

## 26. Permissions
- `notification:rule:view`
- `notification:rule:manage`
- `notification:template:view`
- `notification:template:manage`
- `notification:delivery:view`
- `notification:delivery:retry`
- `notification:provider:manage`

## 27. Closure Condition
一次 NotificationMessage 只有当所有必要 Delivery 均进入最终状态，或根据 FallbackPolicy 已完成替代渠道投递，才算闭环。任何超过最大重试的交易/法律通知必须产生可见 IntegrationTask，禁止静默丢失。
