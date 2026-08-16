# DDD Package Guide

每个 `*-server`：

```text
interfaces/
application/
domain/
infrastructure/
```

Domain 不允许依赖 Spring MVC / MyBatis / Redis / RabbitMQ / Flowable / MinIO。

Application 负责 Use Case、事务边界、Saga 协调。
Infrastructure 负责数据库、MQ、第三方 Provider、缓存等适配。
