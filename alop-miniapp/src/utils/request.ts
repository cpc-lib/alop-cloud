import { API_BASE_URL } from '@/config/env'
import { getToken } from './token'
import type { ApiResponse } from '@/types/api'

export async function request<T>(options: UniApp.RequestOptions): Promise<T> {
  const token = getToken()
  const response = await uni.request({
    ...options,
    url: options.url.startsWith('http') ? options.url : `${API_BASE_URL}${options.url}`,
    header: {
      ...options.header,
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  })

  const body = response.data as ApiResponse<T>
  if (response.statusCode === 401) {
    clearSessionAndLogin()
    throw new Error('登录已失效')
  }
  if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0) {
    throw new Error(body?.message || '请求失败')
  }
  return body.data
}

function clearSessionAndLogin() {
  uni.clearStorageSync()
  uni.reLaunch({ url: '/pages/login/index' })
}
