import { request } from '@/utils/request'

export const listBills = () => request<any>({ url: '/api/billing/bills/me', method: 'GET' })
export const getBill = (id: string) => request<any>({ url: `/api/billing/bills/${id}`, method: 'GET' })
