# CRM DOMAIN SPEC

## 1. Bounded Context / Service
`alop-crm`

## 2. Aggregate Roots
- `Lead`
- `Customer`
- `Opportunity`
- `Viewing`
- `Quotation`

## 3. Owned Tables
- `crm_lead`
- `customer`
- `customer_contact`
- `customer_merge_record`
- `customer_ownership_history`
- `opportunity`
- `opportunity_stage_history`
- `viewing_appointment`
- `viewing_resource_relation`
- `quotation`
- `quotation_version`
- `quotation_item`
- `crm_activity`
- `crm_task`

## 4. Commands
- `CreateLead`
- `AssignLead`
- `ConvertLead`
- `CreateOpportunity`
- `CompleteViewing`
- `CreateQuotationVersion`
- `SendQuotation`
- `AcceptQuotation`
- `ReopenOpportunity`

## 5. Queries
- `Customer360`
- `OpportunityPipeline`
- `LeadPool`

## 6. Produced Events
- `LeadCreated`
- `CustomerCreated`
- `OpportunityStageChanged`
- `ViewingCompleted`
- `QuotationSent`
- `QuotationAccepted`
- `QuotationExpired`

## 7. Permissions
- `crm:lead:manage`
- `crm:customer:view`
- `crm:quotation:create`
- `crm:quotation:approve`

## 8. Invariants
- `Sent quotation version immutable`
- `quote below floor requires special approval or reject`
- `customer merge never deletes history`
- `blacklisted customer cannot reserve/contract without override approval`

## 9. Transaction / Locking
- `optimistic version for customer/opportunity/quotation`

## 10. Idempotency
- `Lead conversion`
- `QuotationVersion number unique`

## 11. Closure Condition
Opportunity reaches WON after agreement path succeeds or LOST with reason; Customer remains active for after-sales/renewal timeline.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.

## V6.4 Notification Contact Data
Customer/CustomerContact remain the source of contact identity. V6.4 adds verification timestamps and billing-contact designation.
- Customer self-service invoice email may use only a verified customer/billing-contact address.
- Admin resend to a different address requires `invoice:email:send`, reason/audit and tenant policy.
- Notification preference does not overwrite Customer contact master data.
