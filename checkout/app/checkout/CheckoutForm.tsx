'use client'

import { useState } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { placeOrder } from './actions'

/** Step 1 collects the address and creates the order; step 2 is Stripe's own card form. */
export function CheckoutForm({ publishableKey }: { publishableKey: string }) {
  const [stripePromise] = useState(() => loadStripe(publishableKey))
  const [placed, setPlaced] = useState<{ orderId: string; clientSecret: string } | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (placed) {
    return (
      <Elements stripe={stripePromise} options={{ clientSecret: placed.clientSecret }}>
        <PayStep orderId={placed.orderId} />
      </Elements>
    )
  }

  return (
    <form action={async (form) => {
      const r = await placeOrder(form)
      if (r.ok) setPlaced(r)
      else setError(r.error)
    }}>
      {['email', 'name', 'street', 'city', 'postcode', 'country'].map((f) => (
        <label key={f}>{f[0].toUpperCase() + f.slice(1)} <input name={f} required /></label>
      ))}
      {error && <p role="alert">{error}</p>}
      <button>Continue to payment</button>
    </form>
  )
}

function PayStep({ orderId }: { orderId: string }) {
  const stripe = useStripe()
  const elements = useElements()
  const [error, setError] = useState<string | null>(null)

  async function pay(e: React.FormEvent) {
    e.preventDefault()
    if (!stripe || !elements) return
    // The card goes from this browser to Stripe. Our servers only ever see the PaymentIntent id.
    const { error } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: `${window.location.origin}/order/${orderId}` },
    })
    if (error) setError(error.message ?? 'The payment did not go through.')
  }

  return (
    <form onSubmit={pay}>
      <PaymentElement />
      {error && <p role="alert">{error}</p>}
      <button disabled={!stripe}>Pay</button>
    </form>
  )
}
