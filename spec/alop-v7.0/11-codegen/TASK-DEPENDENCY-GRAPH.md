# Task Dependency Graph — V7.0

Recommended implementation waves:

```text
Wave 0: TASK-001
Wave 1: TASK-002, TASK-003
Wave 2: TASK-004, TASK-008, TASK-011
Wave 3: TASK-005, TASK-006
Wave 4: TASK-007, TASK-009, TASK-024
Wave 5: TASK-010, TASK-026
Wave 6: TASK-012, TASK-013, TASK-018, TASK-030
Wave 7: TASK-014, TASK-028, TASK-031
Wave 8: TASK-015, TASK-027, TASK-029, TASK-032
Wave 9: TASK-016, TASK-017, TASK-025, TASK-033
Wave 10: TASK-019, TASK-020, TASK-021
Wave 11: TASK-023, then TASK-022 migration capability after hardening
```

- **TASK-001** — TASK-001 — Platform Foundation; depends on: none
- **TASK-002** — TASK-002 — Tenant Core; depends on: TASK-001
- **TASK-003** — TASK-003 — IAM + Organization; depends on: TASK-001, TASK-002
- **TASK-004** — TASK-004 — Asset + Resource Hierarchy; depends on: TASK-001, TASK-002, TASK-003
- **TASK-005** — TASK-005 — ScheduleGuard + Availability; depends on: TASK-004
- **TASK-006** — TASK-006 — Valuation + Offering + Listing; depends on: TASK-004, TASK-005
- **TASK-007** — TASK-007 — Reservation; depends on: TASK-005, TASK-006
- **TASK-008** — TASK-008 — CRM Core; depends on: TASK-001, TASK-002, TASK-003
- **TASK-009** — TASK-009 — Viewing + Quotation; depends on: TASK-006, TASK-008
- **TASK-010** — TASK-010 — Agreement Core; depends on: TASK-007, TASK-009
- **TASK-011** — TASK-011 — Flowable Approval; depends on: TASK-001, TASK-003
- **TASK-012** — TASK-012 — Agreement Sign Saga; depends on: TASK-005, TASK-007, TASK-010, TASK-011
- **TASK-013** — TASK-013 — Billing Engine; depends on: TASK-010
- **TASK-014** — TASK-014 — Finance Core; depends on: TASK-013
- **TASK-015** — TASK-015 — Payment Domain V6.3; depends on: TASK-002, TASK-014
- **TASK-016** — TASK-016 — Invoice; depends on: TASK-014, TASK-015
- **TASK-017** — TASK-017 — Reconciliation + Dunning; depends on: TASK-014, TASK-015, TASK-016
- **TASK-018** — TASK-018 — Operations + Handover; depends on: TASK-004, TASK-010
- **TASK-019** — TASK-019 — CQRS 360 Read Models; depends on: TASK-007, TASK-008, TASK-010, TASK-013, TASK-014, TASK-015, TASK-016, TASK-017, TASK-018
- **TASK-020** — TASK-020 — React Admin; depends on: TASK-019
- **TASK-021** — TASK-021 — UniApp; depends on: TASK-007, TASK-009, TASK-010, TASK-013, TASK-015, TASK-016, TASK-025
- **TASK-022** — TASK-022 — Tenant Dedicated DB Migration; depends on: TASK-002, TASK-023
- **TASK-023** — TASK-023 — Production Hardening; depends on: TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-007, TASK-010, TASK-013, TASK-014, TASK-015, TASK-016, TASK-017, TASK-018, TASK-025
- **TASK-024** — TASK-024 — Utilities + Property Management Fee + Parking Leasing; depends on: TASK-004, TASK-005, TASK-010, TASK-013, TASK-014
- **TASK-025** — TASK-025 — Notification Center + Invoice Email Delivery; depends on: TASK-001, TASK-002, TASK-003, TASK-016
- **TASK-026** — TASK-026 — Agreement Party; depends on: TASK-010
- **TASK-027** — TASK-027 — Security Deposit; depends on: TASK-010, TASK-014, TASK-015
- **TASK-028** — TASK-028 — Utility Usage Period; depends on: TASK-024, TASK-013
- **TASK-029** — TASK-029 — Unidentified Collection; depends on: TASK-014, TASK-017
- **TASK-030** — TASK-030 — Resource Transfer; depends on: TASK-005, TASK-007, TASK-010, TASK-018
- **TASK-031** — TASK-031 — Tax Domain; depends on: TASK-013, TASK-016
- **TASK-032** — TASK-032 — Accounts Payable; depends on: TASK-003, TASK-014, TASK-031
- **TASK-033** — TASK-033 — Owner Settlement; depends on: TASK-014, TASK-032
