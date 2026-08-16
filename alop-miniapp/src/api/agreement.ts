import { request } from '@/utils/request'

export const listAgreements = () => request<any>({ url: '/api/agreements/me', method: 'GET' })
export const getAgreement = (id: string) => request<any>({ url: `/api/agreements/${id}`, method: 'GET' })
