# Accounts Payable (AP) Domain SPEC

## Scope
AP manages money the tenant owes to suppliers, brokers, utility/property vendors and other counterparties.

AR and AP are distinct:
- Receivable: customer owes tenant
- Payable: tenant owes supplier/owner

## Aggregates
Supplier, SupplierInvoice, Payable, PaymentRequest, PayoutOrder.

## Payable
Status: OPEN, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, WRITTEN_OFF.
Invariant: outstanding_amount = payable_amount - paid_amount.

## Flow
SupplierInvoice/source business
-> Payable
-> PaymentRequest
-> Flowable approval
-> PayoutOrder
-> provider/bank
-> SUCCESS / FAILED / UNKNOWN
-> Ledger
-> Payable settlement.

## Accounting
Recognition:
Dr expense/asset
Cr ACCOUNTS_PAYABLE

Payout:
Dr ACCOUNTS_PAYABLE
Cr BANK

## Invariants
- payout <= approved outstanding payable
- duplicate supplier invoices detected
- UNKNOWN payout is queried, not blindly retried
- history uses reversal/adjustment
- strict tenant isolation

## APIs
/api/admin/v1/suppliers
/api/admin/v1/payables
/api/admin/v1/payment-requests
/api/admin/v1/payouts
