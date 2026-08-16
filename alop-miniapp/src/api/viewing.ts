import { request } from '@/utils/request'

export const createViewing = (data?: any) => request<any>({ url: '/api/crm/viewings', method: 'POST', data })
export const listViewings = () => request<any>({ url: '/api/crm/viewings/me', method: 'GET' })
