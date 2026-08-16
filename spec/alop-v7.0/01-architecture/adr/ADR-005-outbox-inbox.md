# ADR-005 Outbox / Inbox

所有跨域状态传播：业务本地事务 + Outbox；Publisher Confirm；消费者 Inbox `(tenant_id,consumer_group,event_id)` 唯一；Business TX 与 Inbox 同事务；失败进入 Retry/DLQ/IntegrationTask。
