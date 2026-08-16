import { request } from '@/utils/request'

export const listResources = () => request<any>({ url: '/api/assets/resources', method: 'GET' })
export const getResource = (id: string) => request<any>({ url: `/api/assets/resources/${id}`, method: 'GET' })
