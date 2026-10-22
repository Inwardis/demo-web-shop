import { Pool } from 'pg'

// Checkout owns the orders schema and writes it directly (Orders DB, JDBC in the model).
const pool = new Pool({ connectionString: process.env.DATABASE_URL ?? 'postgres://webshop:webshop@localhost:5439/webshop' })

export interface OrderLine { sku: string; quantity: number; priceCents: number }
export interface ShipTo { name: string; street: string; city: string; postcode: string; country: string }

export async function createOrder(email: string, shipTo: ShipTo, lines: OrderLine[], currency: string): Promise<{ id: string; totalCents: number }> {
  const id = crypto.randomUUID()
  const totalCents = lines.reduce((sum, l) => sum + l.priceCents * l.quantity, 0)
  const client = await pool.connect()
  try {
    await client.query('BEGIN')
    await client.query(
      'INSERT INTO orders.customer_order (id, email, ship_to, total_cents, currency) VALUES ($1, $2, $3, $4, $5)',
      [id, email, JSON.stringify(shipTo), totalCents, currency],
    )
    for (const l of lines) {
      await client.query(
        'INSERT INTO orders.order_line (order_id, sku, quantity, price_cents) VALUES ($1, $2, $3, $4)',
        [id, l.sku, l.quantity, l.priceCents],
      )
    }
    await client.query('COMMIT')
    return { id, totalCents }
  } catch (e) {
    await client.query('ROLLBACK')
    throw e
  } finally {
    client.release()
  }
}

export async function getOrder(id: string) {
  const { rows } = await pool.query(
    'SELECT id, email, total_cents, currency, payment_status FROM orders.customer_order WHERE id = $1',
    [id],
  )
  return rows[0] as { id: string; email: string; total_cents: string; currency: string; payment_status: string } | undefined
}
