# UniApp 用户端架构

正式目录：

`alop-miniapp/`

## 用户侧业务闭环

```text
Auth
→ Home
→ Resource / Listing
→ Viewing
→ Quotation
→ Reservation
→ Agreement
→ Handover
→ Bill
→ Payment
→ Invoice
→ WorkOrder
→ Notification
→ Mine
```

## 代码分层

```text
src/
├── api/            # 按后端 bounded context 分类
├── components/     # 通用 UI
├── config/
├── constants/
├── hooks/
├── pages/          # 页面
├── store/          # Pinia
├── types/
└── utils/          # request/token/money 等
```

用户端不能自行做资金、合同和排期最终状态判断；最终事实必须来自后端。
