/** Every amount in the shop is integer cents; this is the only place they become text. */
export function formatMoney(cents: number, currency: string): string {
  return new Intl.NumberFormat('en-IE', { style: 'currency', currency }).format(cents / 100)
}
