# Finance Allocation Transaction
1. `BEGIN`.
2. `SELECT collection_record ... FOR UPDATE`.
3. Sort receivable IDs; lock each `receivable ... FOR UPDATE`.
4. Validate allocation >0, <= collection.unallocated, <= receivable.outstanding.
5. Insert payment_allocation.
6. Update Collection allocated/unallocated.
7. Update Receivable allocated/outstanding/status.
8. Create balanced AccountingEntry/Lines.
9. Audit + Outbox.
10. `COMMIT`.
