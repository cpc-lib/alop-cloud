# INVOICE EMAIL DELIVERY SPEC

## 1. Goal
发票开具成功后支持自动向客户邮箱发送电子发票，并支持后台手工重发。发送能力由 `alop-notification` 实现，`alop-invoice` 保存发票发送业务指令与可审计状态。

## 2. Domain ownership
Invoice owns:
- 发票税务事实；
- 本次要把哪张发票发送给哪些业务联系人；
- Delivery Instruction 与发送策略快照。

Notification owns:
- 邮件模板渲染；
- EmailProvider 调用；
- 重试/回执/bounce；
- Delivery attempt。

## 3. Tables
Invoice service adds:
- `invoice_delivery_instruction`
- `invoice_delivery_recipient`

## 4. Delivery Mode
- `NONE`
- `EMAIL_AUTO_AFTER_ISSUE`
- `EMAIL_MANUAL`

InvoiceApplication 可保存默认 delivery mode；最终 `InvoiceDeliveryInstruction` 在发票开具时生成快照。

## 5. InvoiceDeliveryInstruction fields
- `instructionId`
- `invoiceId`
- `applicationId`
- `deliveryType=EMAIL`
- `source=AUTO_AFTER_ISSUE|MANUAL_RESEND`
- `parentInstructionId`
- `templateCode`
- `subjectSnapshot`
- `status`
- `requestedBy`
- `requestedAt`
- `sentAt`
- `notificationMessageId`
- `version`

Status:
`CREATED -> QUEUED -> SENDING -> SENT`
Alternative: `PARTIALLY_SENT / FAILED / CANCELLED`.

## 6. Recipient snapshot
`invoice_delivery_recipient` stores encrypted recipient email and HMAC hash at instruction creation time. Supported role:
- `TO`
- `CC`
- `BCC`

Maximum recipients per instruction should be tenant-configurable; default 10.

## 7. Automatic send flow
`Invoice Provider -> Invoice ISSUED -> PDF/OFD persisted in File Service -> Create InvoiceDeliveryInstruction -> Outbox invoice.invoice.delivery-requested.v1 -> Notification consumes -> fetch instruction through internal API -> render email -> fetch invoice PDF securely by fileId -> send -> NotificationSent/Failed -> Invoice updates instruction status`

Invoice `ISSUED` must not be rolled back because email failed. Email delivery is eventual consistency and operationally retryable.

## 8. Attachments
Default:
- PDF: attach when available;
- OFD: optional based on tenant rule;
- XML/other official format: optional when provider returns it.

Notification service requests file bytes from File Service using internal authenticated API. Do not put PDF bytes or public pre-signed URLs in RabbitMQ events.

If attachment exceeds provider max size:
- use secure temporary download link generated for the intended recipient if tenant policy permits; or
- mark `ATTACHMENT_TOO_LARGE` and require configured fallback.

## 9. Email contents
Recommended template variables:
- tenantBrandName
- customerName
- invoiceNo
- invoiceType
- invoiceAmount
- issuedAt
- agreementNo (optional)
- billNo (optional)
- supportContact

Sensitive bank/tax data should not be fully exposed in body unless explicitly required.

## 10. Manual resend
API: `POST /api/admin/v1/invoices/{invoiceId}/email-deliveries`
Creates a NEW instruction with `source=MANUAL_RESEND` and optional `parentInstructionId`. Never overwrite prior delivery records.

Required permission: `invoice:email:send`.
High-frequency resend is rate limited and audited.

## 11. Customer self-service
Optional API: `POST /api/app/v1/invoices/{invoiceId}/email-deliveries`
Can only send to verified email belonging to current customer or an explicitly authorized billing contact. Client cannot supply arbitrary email address unless business policy requires re-verification.

## 12. Dedup
Auto delivery dedup key:
`tenantId + invoiceId + EMAIL_AUTO_AFTER_ISSUE + recipientSetHash`.

Duplicate `InvoiceIssued` event must not send duplicate auto emails.
Manual resend intentionally creates a new instruction and therefore a new audit record.

## 13. Failure handling
- invalid/missing email -> `FAILED`, stable error + customer/finance task if auto send required;
- Provider 5xx/network -> retry;
- provider result UNKNOWN -> do not immediately resend;
- bounce -> mark delivery `BOUNCED`, optionally suppress address and create CRM task;
- email failure never changes Invoice from `ISSUED` to `FAILED`.

## 14. Invoice page
Admin Invoice Detail must display:
- latest email delivery status;
- TO/CC masked recipient;
- sent time;
- provider;
- retry count;
- bounce/failure reason;
- complete send history;
- manual resend action.
