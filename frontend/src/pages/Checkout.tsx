import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDate, orderTypeLabel, rupees } from '../lib/format'
import type { Cart, Order, OrderType, User } from '../lib/types'
import { Button, Card, EmptyState, ErrorState, FormError, LoadingState, PageTitle, Textarea } from '../components/ui'

const FEES: Record<OrderType, string> = { PICKUP: 'Free', COOK_DELIVERY: '₹30', THIRD_PARTY_DELIVERY: 'Courier quote at checkout (≈ ₹60)' }

export default function Checkout() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { user } = useAuth()
  const [type, setType] = useState<OrderType>('PICKUP')
  const me = useQuery({ queryKey: ['me'], queryFn: () => api.get<User>('/users/me') })
  const cart = useQuery({ queryKey: ['cart'], queryFn: () => api.get<Cart>('/cart') })
  const [address, setAddress] = useState<string | null>(null)
  const addr = address ?? me.data?.address ?? ''

  const place = useMutation({
    mutationFn: () => api.post<Order>('/orders', { orderType: type, deliveryAddress: type === 'PICKUP' ? undefined : addr }),
    onSuccess: (o) => {
      qc.invalidateQueries({ queryKey: ['cart'] })
      qc.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/orders/${o.id}`, { replace: true, state: { placed: true } })
    },
  })

  if (cart.isLoading || me.isLoading) return <LoadingState />
  if (cart.error) return <ErrorState error={cart.error} onRetry={cart.refetch} />
  const c = cart.data!
  if (!c.items.length) return <div className="container-page py-10"><EmptyState emoji="🛒" title="Nothing to check out" action={<Link to="/cooks" className="font-bold text-brand-600">Browse cooks</Link>} /></div>

  const fee = type === 'COOK_DELIVERY' ? 30 : type === 'THIRD_PARTY_DELIVERY' ? 60 : 0
  const needsAddress = type !== 'PICKUP'
  return (
    <div className="container-page py-8">
      <PageTitle title="Checkout" subtitle={`Order from ${c.cookName}`} />
      <div className="grid gap-6 md:grid-cols-[1fr_340px]">
        <div className="space-y-4">
          <Card className="p-5">
            <h2 className="mb-3 text-lg">How would you like it?</h2>
            <div className="space-y-2" role="radiogroup" aria-label="Fulfilment">
              {(Object.keys(orderTypeLabel) as OrderType[]).map((t) => (
                <label key={t} className={`flex cursor-pointer items-center justify-between rounded-xl border p-3 ${type === t ? 'border-brand-400 bg-brand-50' : 'border-cream-300'}`}>
                  <span className="flex items-center gap-3"><input type="radio" name="type" checked={type === t} onChange={() => setType(t)} /><b>{orderTypeLabel[t]}</b></span>
                  <span className="text-sm text-ink-500">{FEES[t]}</span>
                </label>
              ))}
            </div>
            {needsAddress && <Textarea className="mt-4" label="Delivery address" rows={3} value={addr} onChange={(e) => setAddress(e.target.value)} placeholder="Flat, building, street, area" />}
            {type === 'PICKUP' && <p className="mt-3 text-sm text-ink-500">The pickup address is shared once {c.cookName} confirms your order.</p>}
          </Card>
          <Card className="p-5">
            <h2 className="mb-1 text-lg">Payment</h2>
            <p className="text-sm text-ink-500">Demo payment: your order is paid instantly with a mock provider. No real money moves.</p>
          </Card>
        </div>
        <Card className="h-fit p-5">
          <h2 className="mb-3 text-lg">Order summary</h2>
          <ul className="space-y-1 text-sm">
            {c.items.map((i) => <li key={i.id} className="flex justify-between"><span>{i.quantity} × {i.dishName} <span className="text-ink-500">({formatDate(i.availableDate, { weekday: 'short' })})</span></span><b>{rupees(i.lineTotal)}</b></li>)}
          </ul>
          <div className="mt-3 space-y-1 border-t border-cream-200 pt-3 text-sm">
            <div className="flex justify-between"><span>Subtotal</span><span>{rupees(c.subtotal)}</span></div>
            <div className="flex justify-between"><span>Delivery</span><span>{fee ? rupees(fee) : 'Free'}</span></div>
            <div className="flex justify-between text-base font-extrabold"><span>Total</span><span>{rupees(c.subtotal + fee)}</span></div>
          </div>
          <FormError error={place.error} />
          <Button className="mt-4 w-full" loading={place.isPending} disabled={needsAddress && !addr.trim() && !user?.address} onClick={() => place.mutate()}>Place order</Button>
        </Card>
      </div>
    </div>
  )
}
