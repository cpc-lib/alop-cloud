import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'

export function useAuth() {
  const store = useAuthStore()
  return {
    isLoggedIn: computed(() => !!store.token),
    logout: store.logout
  }
}
