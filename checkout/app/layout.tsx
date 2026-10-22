/**
 * Checkout — the customer-facing shop: basket, address, order confirmation. Server-rendered.
 * Starts a payment through Payments and never sees card data.
 *
 * @inwardis a842702c-58f9-4957-beb7-5e69d11c5b2c Checkout
 */
import type { ReactNode } from 'react'
import Link from 'next/link'
import './globals.css'

export const metadata = { title: 'Trailhead Supply' }

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <header>
          <Link href="/" className="brand">Trailhead Supply</Link>
          <Link href="/basket">Basket</Link>
        </header>
        <main>{children}</main>
      </body>
    </html>
  )
}
