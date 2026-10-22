import { notFound } from 'next/navigation'
import { getOrder } from '@/lib/orders'
import { capturePayment } from '@/lib/payments'
import { formatMoney } from '@/lib/money'

const MESSAGES: Record<string, string> = {
  CAPTURED: 'Thank you — your order is confirmed.',
  CAPTURED_FOR_REVIEW: 'Thank you — your order is confirmed. We are giving it a quick check before it ships.',
  DECLINED: 'We could not take this payment. Nothing has been charged.',
}

/** Stripe returns the customer here after the card step; this is where the order is confirmed. */
export default async function OrderPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  const order = await getOrder(id)
  if (!order) notFound()

  const status = order.payment_status === 'AUTHORISED' ? await capturePayment(id) : order.payment_status
  return (
    <>
      <h2>Order {id.slice(0, 8)}</h2>
      <p>{MESSAGES[status] ?? `Payment status: ${status}`}</p>
      <p>Total {formatMoney(Number(order.total_cents), order.currency)} — a receipt is on its way to {order.email}.</p>
    </>
  )
}
