import { CheckoutForm } from './CheckoutForm'

export default function CheckoutPage() {
  return (
    <>
      <h2>Checkout</h2>
      <CheckoutForm publishableKey={process.env.STRIPE_PUBLISHABLE_KEY ?? 'pk_test_replace_me'} />
    </>
  )
}
