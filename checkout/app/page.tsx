import { listProducts } from '@/lib/catalog'
import { formatMoney } from '@/lib/money'
import { addToBasket } from './basket/actions'

// Rendered per request; Catalog's answers are still cached for 60 s by the fetch itself.
export const dynamic = 'force-dynamic'

export default async function Home() {
  const products = await listProducts()
  return (
    <div className="grid">
      {products.map((p) => (
        <form key={p.sku} action={addToBasket} className="card">
          <h3>{p.name}</h3>
          <p>{p.description}</p>
          <p><strong>{formatMoney(p.priceCents, p.currency)}</strong>{p.stock === 0 && ' — sold out'}</p>
          <input type="hidden" name="sku" value={p.sku} />
          <button disabled={p.stock === 0}>Add to basket</button>
        </form>
      ))}
    </div>
  )
}
