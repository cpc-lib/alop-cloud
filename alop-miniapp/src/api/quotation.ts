import { request } from '@/utils/request'

export const getQuotation = (id: string) => request<any>({ url: `/api/crm/quotations/${id}`, method: 'GET' })
