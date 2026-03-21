/** 中国大陆 11 位手机号 */
export function isValidCnMobile(phone: string): boolean {
  const s = (phone || '').trim()
  return /^1\d{10}$/.test(s)
}

export function normalizeCnMobile(phone: string): string {
  return (phone || '').trim().replace(/\s/g, '')
}
