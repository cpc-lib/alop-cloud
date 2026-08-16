# V7.0 Freeze Policy

## Purpose
Stop architecture drift while allowing implementation detail to evolve safely.

## Changes allowed without architecture ADR
- add read-only query endpoints that do not expose new sensitive data;
- add non-breaking response fields;
- add indexes based on measured queries;
- add metrics/alerts/tests;
- add provider adapters behind an existing SPI;
- add tenant configuration values inside an existing capability.

## Changes requiring ADR
- new bounded context or service;
- aggregate ownership changes;
- moving a financial fact between services;
- changing ScheduleGuard/ConflictGroup semantics;
- new payment/invoice/payout truth semantics;
- breaking event schema or API;
- sharding-key/routing-model changes;
- changing Agreement CLOSED criteria;
- changing accounting posting policy.

## Breaking contract versioning
- REST: introduce `/api/v2` or explicit compatible deprecation plan.
- Event: publish new major routing key `...v2`; never silently reinterpret v1.
- DDL: Expand -> Migrate -> Contract.
- Enum: unknown future values must fail safely or map to UNKNOWN only where SPEC allows it.
