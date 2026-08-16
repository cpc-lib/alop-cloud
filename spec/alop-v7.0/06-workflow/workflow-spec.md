# Workflow SPEC

Processes: asset-entry-approval, valuation-approval, special-price-approval, agreement-approval, agreement-change-approval, refund-approval, manual-collection-approval, invoice-red-flush-approval, finance-adjustment-approval, reconciliation-exception-approval.

Rules:
- Flowable owns human task orchestration only.
- Business service stores business state and revalidates all invariants when workflow completes.
- Business relation records definition key/version/instance id.
- Variables are compact: tenantId, businessId, amountLevel, discountLevel, creditLevel; never whole aggregate JSON.
