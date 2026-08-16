# ADR-006 Agreement Sign Saga

Agreement 与 Reservation/Occupancy 属不同服务。签约使用可恢复 Saga：Agreement PREPARING -> Asset CommitReservation -> Asset COMMITTED -> Agreement SIGNED。若 Asset 已提交而 Agreement 本地提交失败，进入 COMPENSATING，调用幂等 ReleaseCommittedReservation，禁止直接删除 Occupancy。
