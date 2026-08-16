# Module Definition of Done Matrix

Every backend module must have:
- compiling Java 21 code;
- Flyway migration and migration test;
- OpenAPI contract test;
- tenant isolation integration test;
- permission/audit implementation;
- idempotency for high-risk writes;
- Outbox/Inbox where events cross service boundaries;
- metrics and structured trace fields;
- README with dependencies, queues, jobs, failure recovery;
- unit + integration + domain failure-path tests;
- SPEC implementation mapping.

Additional gates:
- Asset/Reservation: ScheduleGuard + conflict scope concurrency tests.
- Agreement: snapshot, Flowable adapter, sign/resource-transfer Saga compensation.
- Billing: exact rounding/proration/tax/utility trace tests.
- Payment: callback security, UNKNOWN, late-success, refund reservation.
- Finance: allocation lock order, deposit, unidentified cash, ledger balance, reconciliation.
- Invoice: quota, provider UNKNOWN, partial/full red flush policy, email delivery independence.
- AP/Owner Settlement: payout UNKNOWN and immutable closed settlement adjustment.
