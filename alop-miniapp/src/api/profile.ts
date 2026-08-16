import { request } from '@/utils/request'

export const getProfile = () => request<any>({ url: '/api/iam/me/profile', method: 'GET' })
