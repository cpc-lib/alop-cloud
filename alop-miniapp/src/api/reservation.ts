import { request } from '@/utils/request'

export const createReservation = (data?: any) => request<any>({ url: '/api/reservations', method: 'POST', data })
export const listReservations = () => request<any>({ url: '/api/reservations/me', method: 'GET' })
