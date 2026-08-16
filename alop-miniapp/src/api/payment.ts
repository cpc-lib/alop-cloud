import { request } from '@/utils/request'

export const createPayment = (data?: any) => request<any>({ url: '/api/payments', method: 'POST', data })
export const getPayment = (id: string) => request<any>({ url: `/api/payments/${id}`, method: 'GET' })
