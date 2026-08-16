import { request } from '@/utils/request'

export const listInvoices = () => request<any>({ url: '/api/invoices/me', method: 'GET' })
export const applyInvoice = (data?: any) => request<any>({ url: '/api/invoices/applications', method: 'POST', data })
