# Target Architecture

```text
React Admin / UniApp
        ↓
     Gateway
        ↓
Tenant/IAM/Organization
        ↓
Business Services
        ↓
MySQL / Redis / RabbitMQ / ES / MinIO
```

业务服务内：

```text
interfaces
    ↓
application
    ↓
domain
    ↑
infrastructure
```

跨服务：

```text
Business TX
├── business rows
└── outbox
     ↓
RabbitMQ
     ↓
Inbox + Consumer Local TX
```
