// Catalog's API. Checkout never reads the catalog schema directly.
const CATALOG_URL = process.env.CATALOG_URL ?? 'http://localhost:8101'

export interface Product {
  sku: string
  name: string
  description: string
  priceCents: number
  currency: string
  stock: number
}

export async function listProducts(): Promise<Product[]> {
  const res = await fetch(`${CATALOG_URL}/products`, { next: { revalidate: 60 } })
  if (!res.ok) throw new Error(`catalog: ${res.status}`)
  return res.json()
}

export async function getProduct(sku: string): Promise<Product | null> {
  const res = await fetch(`${CATALOG_URL}/products/${encodeURIComponent(sku)}`, { next: { revalidate: 60 } })
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`catalog: ${res.status}`)
  return res.json()
}

/** Throws OutOfStock when Catalog answers 409 — the basket asked for more than is left. */
export class OutOfStock extends Error {}

export async function reserve(sku: string, quantity: number): Promise<void> {
  const res = await fetch(`${CATALOG_URL}/products/${encodeURIComponent(sku)}/reserve`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ quantity }),
  })
  if (res.status === 409) throw new OutOfStock(sku)
  if (!res.ok) throw new Error(`catalog: ${res.status}`)
}
