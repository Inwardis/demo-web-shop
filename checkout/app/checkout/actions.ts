'use server'

import { readBasket, writeBasket } from '@/lib/basket'
import { getProduct, reserve, OutOfStock } from '@/lib/catalog'
import { createOrder } from '@/lib/orders'
import { startPayment } from '@/lib/payments'

export type PlaceOrderResult =
  | { ok: true; orderId: string; clientSecret: string }
  | { ok: false; error: string }

/** Prices come from Catalog, not the browser; stock is reserved before the order exists. */
export async function placeOrder(form: FormData): Promise<PlaceOrderResult> {
  const basket = await readBasket()
  const lines = []
  for (const [sku, quantity] of Object.entries(basket)) {
    const product = await getProduct(sku)
    if (!product) continue
    lines.push({ sku, quantity, priceCents: product.priceCents })
  }
  if (lines.length === 0) return { ok: false, error: 'Your basket is empty.' }

  try {
    for (const l of lines) await reserve(l.sku, l.quantity)
  } catch (e) {
    if (e instanceof OutOfStock) return { ok: false, error: `Sorry — ${e.message} sold out while you were shopping.` }
    throw e
  }

  const shipTo = {
    name: String(form.get('name')),
    street: String(form.get('street')),
    city: String(form.get('city')),
    postcode: String(form.get('postcode')),
    country: String(form.get('country')),
  }
  const order = await createOrder(String(form.get('email')), shipTo, lines, 'EUR')
  const clientSecret = await startPayment(order.id, order.totalCents, 'eur')
  await writeBasket({})
  return { ok: true, orderId: order.id, clientSecret }
}
