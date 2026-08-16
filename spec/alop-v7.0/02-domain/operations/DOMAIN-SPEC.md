# OPERATIONS DOMAIN SPEC

## 1. Bounded Context / Service
`alop-asset (phase1) / alop-operation (future)`

## 2. Aggregate Roots
- `OperationWorkOrder`
- `RenovationOrder`
- `MaintenanceOrder`
- `Complaint`

## 3. Owned Tables
- `operation_work_order`
- `operation_work_order_log`
- `renovation_order`
- `maintenance_order`
- `complaint`

## 4. Commands
- `CreateWorkOrder`
- `AssignWorkOrder`
- `CompleteWorkOrder`
- `StartRenovation`
- `CompleteRenovation`
- `CreateComplaint`

## 5. Queries
- `ListOpenWorkOrders`
- `GetWorkOrder`

## 6. Produced Events
- `WorkOrderCreated`
- `WorkOrderClosed`
- `RenovationStarted`
- `RenovationCompleted`

## 7. Permissions
- `operation:workorder:manage`
- `operation:renovation:approve`

## 8. Invariants
- `maintenance cannot silently break existing lease`
- `renovation with future lease conflict creates conflict task`
- `SLA violations generate operational events`

## 9. Transaction / Locking
- `ScheduleGuard when work order blocks resource schedule`

## 10. Idempotency
- `external ticket id optional unique`

## 11. Closure Condition
Work order closed after execution, verification, attachments/evidence and schedule block release when applicable.

## 12. Required Application Layer Pattern
- Controller only validates DTO and dispatches Command/Query.
- Application loads aggregates, checks tenant/permission, starts local transaction, invokes Domain behavior, saves repository and Outbox.
- Domain contains state transition and invariant rules; no MyBatis/Redis/RabbitMQ/Flowable dependencies.
- Query side may use projection/read mapper directly under Tenant scope.

## 13. Failure Handling
- Domain conflict returns stable business error code; do not translate to generic RuntimeException.
- Temporary DB/external errors are retryable only when operation is idempotent.
- Cross-domain partial success creates/reuses persistent Saga/IntegrationTask; no manual SQL repair.

## 14. Audit & Metrics
- State-changing high-risk commands write Audit in the same local transaction or reliable Outbox.
- Metrics at minimum: success, failure by domain code, latency, optimistic/deadlock conflicts, backlog where applicable.

## 15. Mandatory Tests
- Happy path.
- Invalid state transition.
- Tenant A/B isolation.
- Idempotent duplicate request/event.
- Persistence integration with MySQL Testcontainers.
- Domain tests without Spring.


## 16. V6.3 水电/车位交接要求
- MOVE_IN 必须按租户策略采集 WATER/ELECTRICITY 初始读数；表计读数关联 HandoverOrder 与证据附件。
- MOVE_OUT 必须采集最终读数并等待最终水电费用进入 Bill/Receivable，除非该合同明确采用固定水电费模式。
- 车位交接记录停车位、车辆绑定、门禁/遥控器/充电设备状态。
- Agreement CLOSED 检查必须包含未完成 Utility Settlement 与 Active ParkingVehicleBinding。

## V6.4 WorkOrder Notification Ownership
Operations owns SLA/business triggers such as `operations.work-order.sla-violated.v1`, assignment changes and customer-visible completion facts. Notification owns delivery channels and retries.
