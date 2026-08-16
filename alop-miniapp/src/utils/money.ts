export function formatMoney(amount?: string, currency = 'CNY'): string {
  if (!amount) return '0.00'
  return `${currency === 'CNY' ? '¥' : currency + ' '}${amount}`
}
