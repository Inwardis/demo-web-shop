'use server'

import { redirect } from 'next/navigation'
import { readBasket, writeBasket } from '@/lib/basket'

export async function addToBasket(form: FormData) {
  const sku = String(form.get('sku'))
  const basket = await readBasket()
  basket[sku] = (basket[sku] ?? 0) + 1
  await writeBasket(basket)
  redirect('/basket')
}

export async function removeFromBasket(form: FormData) {
  const basket = await readBasket()
  delete basket[String(form.get('sku'))]
  await writeBasket(basket)
}
