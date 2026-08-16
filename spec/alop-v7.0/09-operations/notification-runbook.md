# NOTIFICATION / INVOICE EMAIL RUNBOOK

## Dashboards
Monitor:
- pending/retry/failed delivery count;
- SMS reject rate;
- email bounce rate;
- provider latency/error rate;
- invoice email failure backlog;
- IntegrationTask backlog.

## Incident: invoice issued but customer did not receive email
1. Confirm Invoice status is `ISSUED`.
2. Open invoice delivery history.
3. Check DeliveryInstruction status.
4. If no instruction: inspect invoice Outbox / auto-delivery policy.
5. If instruction QUEUED/SENDING: inspect notification queue/backlog.
6. If FAILED: inspect stable failure code.
7. If BOUNCED: verify recipient address with customer; do not blindly retry same suppressed address.
8. Use manual resend only after correcting recipient/config; every resend must be audited.

Never modify invoice tax status to repair an email issue.

## Incident: duplicate SMS/email complaint
- search logical dedup key and provider request no;
- verify duplicate business event vs duplicate provider delivery;
- inspect Inbox and notification message unique key;
- do not delete delivery history;
- create defect/repair task if unique invariant violated.

## Incident: Provider UNKNOWN
Do not immediately create a second delivery. Use provider status query/receipt when supported; otherwise wait configured uncertainty window before manual intervention.

## Incident: high email bounce
- inspect provider/domain/sender reputation;
- suppress confirmed invalid recipient hashes;
- create CRM data-quality tasks for affected customers;
- do not expose full addresses in incident channel/logs.

## Tenant provider outage
Tenant-specific provider failure must be bulkheaded. If rule allows fallback to platform provider, execute only according to configured fallback and compliance policy.
