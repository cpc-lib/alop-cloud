# Concurrency Test Matrix

1. 100 threads reserve same tenant/resource/time => exactly 1 effective reservation, 99 conflict/busy.
2. Redis unavailable => same correctness via MySQL ScheduleGuard.
3. Multi-resource A+B+C where B conflicts => zero reservation items committed.
4. WholeUnit vs Room conflict => mutual exclusion both directions.
5. Reservation expiry vs deposit confirmation => only EXPIRED or CONFIRMED wins.
6. Allocation 100 available, concurrent 80+80 => total allocation <=100.
7. Invoice quota 100, concurrent 80+80 => total RESERVED/CONFIRMED <=100.
8. Duplicate PaymentSucceeded event x100 => one Collection, one Ledger posting.
