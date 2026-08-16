export interface Money {
  amount: string
  currency: string
}

export interface MembershipContext {
  tenantId: string
  membershipId: string
  userId: string
}
