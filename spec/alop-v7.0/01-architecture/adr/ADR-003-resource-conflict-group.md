# ADR-003 Resource Conflict Group

解决整租/分租、父子资源互斥。`MUTUAL_EXCLUSIVE` 与 `PARENT_CHILD_EXCLUSIVE` 组内任意有效 Reservation/Occupancy 都会阻止冲突资源在重叠时间再次出租。
