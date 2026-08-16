# ADR-008 Invoice Quota Reservation

开票依据为有效 Allocation。并发开票必须先在 Finance 预占 InvoiceQuotaReservation。Provider UNKNOWN 时额度保持 RESERVED；明确失败才 RELEASE；开票成功 CONFIRMED；红冲成功恢复额度。
