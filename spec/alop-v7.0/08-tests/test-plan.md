# Test Plan

## Mandatory suites
- Domain unit tests without Spring.
- Repository/MySQL integration via Testcontainers.
- Tenant A/B isolation for every repository and endpoint class.
- API contract tests against OpenAPI.
- MQ duplicate/out-of-order tests with Inbox.
- Payment/Refund/Invoice UNKNOWN recovery tests.
- Finance precision and double-entry tests.
- E2E business closed-loop scenarios.

## Coverage targets
- Domain >= 90% line/branch coverage for invariants/state transitions.
- Application >= 80% for orchestration/error paths.
- Coverage is not a substitute for scenario tests.

## V6.3专项
Meter reading versioning/anomaly, shared allocation, tariff tiers, property fee area snapshot, parking ScheduleGuard concurrency, vehicle binding history, move-out final utility settlement。


## Payment V6.3
专项测试必须加载 `payment.md`，并覆盖 PaymentOrder/Attempt/Transaction、UNKNOWN、晚到成功、多租户商户隔离、退款 Finance Reservation 和 100 次重复回调。
