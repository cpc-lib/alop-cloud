# Resource Conflict SQL
```sql
SELECT id
FROM resource_occupancy
WHERE tenant_id = :tenantId
  AND resource_unit_id = :resourceId
  AND status IN ('PLANNED','ACTIVE')
  AND start_time < :newEnd
  AND end_time > :newStart
LIMIT 1;
```
Reservation conflict uses the same overlap predicate and joins `reservation` status in HELD/CONFIRMED. Before any check, lock target + conflict-group `resource_schedule_guard` rows in resource ID ascending order.
