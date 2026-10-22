import Link from 'next/link'
import { readBasket } from '@/lib/basket'
import { getProduct } from '@/lib/catalog'
import { formatMoney } from '@/lib/money'
import { removeFromBasket } from './actions'

export default async function BasketPage() {
  const basket = await readBasket()
  const lines = (await Promise.all(
    Object.entries(basket).map(async ([sku, quantity]) => ({ product: await getProduct(sku), quantity })),
  )).filter((l) => l.product !== null)

  if (lines.length === 0) return <p>Your basket is empty. <Link href="/">Keep looking</Link>.</p>

  const total = lines.reduce((sum, l) => sum + l.product!.priceCents * l.quantity, 0)
  return (
    <>
      <h2>Basket</h2>
      {lines.map(({ product, quantity }) => (
        <form key={product!.sku} action={removeFromBasket} className="card">
          {quantity} × {product!.name} — {formatMoney(product!.priceCents * quantity, product!.currency)}
          <input type="hidden" name="sku" value={product!.sku} /> <button>Remove</button>
        </form>
      ))}
      <p><strong>Total {formatMoney(total, 'EUR')}</strong></p>
      <Link href="/checkout">Go to checkout</Link>
    </>
  )
}
