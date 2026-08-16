import { defineStore } from 'pinia'
import { getToken, setToken, clearToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken()
  }),
  actions: {
    login(token: string) {
      setToken(token)
      this.token = token
    },
    logout() {
      clearToken()
      this.token = ''
    }
  }
})
