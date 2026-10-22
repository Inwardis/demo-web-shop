import { cookies } from 'next/headers'

// The basket is a cookie of sku → quantity. Prices are looked up again at checkout,
// never trusted from the browser.
export type Basket = Record<string, number>

const COOKIE = 'basket'

export async function readBasket(): Promise<Basket> {
  const raw = (await cookies()).get(COOKIE)?.value
  try {
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export async function writeBasket(basket: Basket): Promise<void> {
  (await cookies()).set(COOKIE, JSON.stringify(basket), { httpOnly: true, sameSite: 'lax', path: '/' })
}
