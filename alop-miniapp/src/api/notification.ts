import { request } from '@/utils/request'

export const listNotifications = () => request<any>({ url: '/api/notifications/me', method: 'GET' })
