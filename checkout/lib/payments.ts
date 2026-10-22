// Payments' API. Checkout starts and confirms a payment; it never sees card data —
// the card goes from the customer's browser straight to Stripe (ADR-003).
const PAYMENTS_URL = process.env.PAYMENTS_URL ?? 'http://localhost:8102'

export async function startPayment(orderId: string, amountCents: number, currency: string): Promise<string> {
  const res = await fetch(`${PAYMENTS_URL}/payments`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ orderId, amountCents, currency }),
  })
  if (!res.ok) throw new Error(`payments: ${res.status}`)
  return (await res.json()).clientSecret
}

export type CaptureResult = 'CAPTURED' | 'CAPTURED_FOR_REVIEW' | 'DECLINED'

export async function capturePayment(orderId: string): Promise<CaptureResult> {
  const res = await fetch(`${PAYMENTS_URL}/payments/${orderId}/capture`, { method: 'POST' })
  if (!res.ok) throw new Error(`payments: ${res.status}`)
  return (await res.json()).result
}
