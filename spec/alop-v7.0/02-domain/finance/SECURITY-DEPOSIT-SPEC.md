# Security Deposit SPEC

## Goal
Security deposits are liabilities with their own lifecycle, not ordinary rental income.

## Aggregate
`SecurityDepositAccount`

Fields:
- id, tenant_id, agreement_id, customer_id
- currency
- required_amount, received_amount, deducted_amount
- refunded_amount, reserved_refund_amount, available_balance
- status, version

Status:
REQUIRED -> PARTIALLY_PAID -> PAID -> PARTIALLY_DEDUCTED -> REFUNDING -> REFUNDED -> CLOSED.

## Transactions
Append-only `security_deposit_transaction`:
REQUIREMENT_CREATED, RECEIPT, ADDITIONAL_RECEIPT, DEDUCTION, REFUND_RESERVATION,
REFUND_SUCCESS, REFUND_RELEASE, ADJUSTMENT, REVERSAL.

## Invariant
`available_balance = received_amount - deducted_amount - refunded_amount - reserved_refund_amount`

It must never be negative.

## Accounting
Deposit receipt:
- Dr BANK
- Cr DEPOSIT_LIABILITY

Deposit deduction against valid Receivable:
- Dr DEPOSIT_LIABILITY
- Cr ACCOUNTS_RECEIVABLE

Deposit refund:
- Dr DEPOSIT_LIABILITY
- Cr BANK

Never post deposit receipt to RENT_INCOME.

## Move-out settlement
MOVE_OUT complete
-> final rent/utility/property/parking/damage receivables
-> lock deposit + receivables
-> allocate approved deductions
-> reserve remaining refund
-> execute refund
-> ledger posting
-> close only when balance is zero and no in-flight refund exists.

## Commands
CreateDepositAccountCommand, RecordDepositReceiptCommand, DeductDepositCommand,
ApplyDepositRefundCommand, ConfirmDepositRefundCommand, ReverseDepositAllocationCommand,
CloseDepositAccountCommand.

## Errors
DEPOSIT_BALANCE_INSUFFICIENT, DEPOSIT_REFUND_IN_PROGRESS, DEPOSIT_NOT_SETTLEABLE,
DEPOSIT_ALREADY_CLOSED.

## Tests
partial payment/top-up; damage/rent deduction; concurrent refund reservation;
refund UNKNOWN retains reservation; Agreement CLOSED blocked while deposit unsettled.
