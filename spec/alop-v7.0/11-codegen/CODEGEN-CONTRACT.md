# V7.0 Codegen Contract

## Required Java package layers
`interfaces -> application -> domain <- infrastructure`

- `interfaces`: REST/MQ adapters, validation only.
- `application`: use-case orchestration, transactions, permission prechecks.
- `domain`: aggregates, entities, VOs, domain services, policies, repository ports, domain events; no MyBatis/Redis/Rabbit/Flowable SDK dependency.
- `infrastructure`: MyBatis repositories, provider SDKs, MQ publisher/consumer adapters, Redis, MinIO, ES, Feign.

## Write-use-case implementation template
1. Resolve authenticated TenantContext.
2. Check feature/permission/data scope.
3. Validate idempotency where required.
4. Load/lock required aggregate/fact rows in the defined order.
5. Invoke aggregate/domain policy; never set status directly.
6. Persist local state.
7. Persist audit + outbox in the same transaction when applicable.
8. Commit.
9. External/eventual work after commit unless a documented Saga/TCC step says otherwise.

## Query implementation template
- use task-specific ReadMapper/Projection;
- tenant predicate is mandatory;
- no N+1 Feign loop;
- no domain aggregate load for reporting lists;
- sensitive fields must be masked according to field permission.

## Generated output acceptance
No `TODO`, no pseudocode, no omitted exception branches, no generated public `setStatus()`, no raw `double` money, no arbitrary `@IgnoreTenant`.
