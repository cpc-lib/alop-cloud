# ADR-004 Finance Boundary

`Receivable + Collection + Allocation + Ledger + Reconciliation + Dunning + InvoiceQuota` 归 Finance Context，使资金核销和账务可以用单库本地强事务完成。Billing 只计算 BillingPlan/Bill。
