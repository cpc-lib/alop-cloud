# ADR-002 Resource Schedule Guard

**Decision:** 每个 ResourceUnit 建一条 `resource_schedule_guard`。Reservation/Occupancy/AvailabilityBlock/Repair/Renovation/SaleLock 修改前，锁目标资源及 ConflictGroup 相关资源，按 ID 升序 `FOR UPDATE`。

**Why:** Redis 锁不能提供网络分区/锁过期场景下的最终正确性；MySQL 行锁作为资源排期串行化点。
