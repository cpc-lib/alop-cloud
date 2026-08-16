import { defineStore } from 'pinia'
import type { MembershipContext } from '@/types/business'

export const useMembershipStore = defineStore('membership', {
  state: () => ({
    current: null as MembershipContext | null
  }),
  actions: {
    setMembership(value: MembershipContext) {
      this.current = value
    }
  }
})
