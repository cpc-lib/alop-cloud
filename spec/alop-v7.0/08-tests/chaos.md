# Chaos / Failure Tests
- Redis down: no oversell.
- RabbitMQ down: local business transaction commits with Outbox PENDING; recovers later.
- ES down: MySQL write succeeds; index catches up later.
- Provider timeout after request: UNKNOWN and query, no duplicate side effect.
- App crashes between Saga steps: persisted process resumes/compensates.
- TenantContext missing: request fails closed, never executes unscoped SQL.
