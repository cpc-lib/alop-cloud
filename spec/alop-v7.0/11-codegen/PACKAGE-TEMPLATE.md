# Service Package Template

```text
com.company.alop.<context>
├── interfaces
│   ├── rest/admin
│   ├── rest/app
│   ├── rest/internal
│   ├── mq
│   ├── request
│   └── response
├── application
│   ├── command
│   ├── query
│   ├── handler
│   ├── service
│   └── assembler
├── domain
│   ├── aggregate
│   ├── entity
│   ├── valueobject
│   ├── repository
│   ├── service
│   ├── policy
│   ├── specification
│   ├── event
│   └── exception
└── infrastructure
    ├── persistence/{dataobject,mapper,repository,converter}
    ├── client
    ├── mq
    ├── cache
    ├── external
    └── config
```
