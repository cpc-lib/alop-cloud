import { request } from '@/utils/request'

export const login = (data?: any) => request<any>({ url: '/api/iam/auth/login', method: 'POST', data })
export const me = () => request<any>({ url: '/api/iam/me', method: 'GET' })
