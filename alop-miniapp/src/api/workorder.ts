import { request } from '@/utils/request'

export const listWorkOrders = () => request<any>({ url: '/api/operations/work-orders/me', method: 'GET' })
export const createWorkOrder = (data?: any) => request<any>({ url: '/api/operations/work-orders', method: 'POST', data })
