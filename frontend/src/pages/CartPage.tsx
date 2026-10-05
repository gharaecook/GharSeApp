import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { formatDate, rupees } from '../lib/format'
import type { Cart } from '../lib/types'
import { Button, Card, EmptyState, ErrorState, FormError, LoadingState, PageTitle } from '../components/ui'

export default function CartPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const cart = useQuery({ queryKey: ['cart'], queryFn: () => api.get<Cart>('/cart') })
  const set = (c: Cart) => qc.setQueryData(['cart'], c)
  const update = useMutation({ mutationFn: (v: { id: string; quantity: number }) => api.put<Cart>(`/cart/items/${v.id}`, { quantity: v.quantity }), onSuccess: set })
  const remove = useMutation({ mutationFn: (id: string) => api.del<Cart>(`/cart/items/${id}`), onSuccess: set })
  const clear = useMutation({ mutationFn: () => api.del('/cart'), onSuccess: () => qc.invalidateQueries({ queryKey: ['cart'] }) })

  if (cart.isLoading) return <LoadingState />
  if (cart.error) return <ErrorState error={cart.error} onRetry={cart.refetch} />
  const c = cart.data!
  if (!c.items.length) {
    return <div className="container-page py-10"><EmptyState emoji="🛒" title="Your cart is empty" message="Find a Home Cook and add something delicious." action={<Link to="/cooks" className="rounded-xl bg-brand-500 px-5 py-2.5 font-bold text-white">Browse Home Cooks</Link>} /></div>
  }
  const blocked = c.items.some((i) => !i.orderingOpen || i.quantity > i.remaining)

  return (
    <div className="container-page py-8">
      <PageTitle title="Your cart" subtitle={c.cookName ? `Food from ${c.cookName}` : undefined}
        action={<Button variant="ghost" onClick={() => clear.mutate()} loading={clear.isPending}>Clear cart</Button>} />
      <div className="grid gap-6 md:grid-cols-[1fr_320px]">
        <ul className="space-y-3">
          {c.items.map((i) => (
            <li key={i.id}>
              <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
                <div>
                  <p className="font-extrabold">{i.dishName}</p>
                  <p className="text-sm text-ink-500">{i.batchName} · {formatDate(i.availableDate, { weekday: 'short', day: 'numeric', month: 'short' })}</p>
                  {!i.orderingOpen && <p className="text-sm font-bold text-red-600">Ordering for this batch has closed</p>}
                  {i.orderingOpen && i.quantity > i.remaining && <p className="text-sm font-bold text-red-600">Only {i.remaining} left</p>}
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex items-center rounded-xl border border-cream-300">
                    <button aria-label={`Decrease ${i.dishName}`} className="px-3 py-1.5 font-bold" disabled={i.quantity <= 1 || update.isPending} onClick={() => update.mutate({ id: i.id, quantity: i.quantity - 1 })}>−</button>
                    <span className="w-6 text-center text-sm font-bold">{i.quantity}</span>
                    <button aria-label={`Increase ${i.dishName}`} className="px-3 py-1.5 font-bold" disabled={update.isPending} onClick={() => update.mutate({ id: i.id, quantity: i.quantity + 1 })}>+</button>
                  </div>
                  <span className="w-20 text-right font-extrabold">{rupees(i.lineTotal)}</span>
                  <button aria-label={`Remove ${i.dishName}`} className="text-ink-300 hover:text-red-600" onClick={() => remove.mutate(i.id)}>✕</button>
                </div>
              </Card>
            </li>
          ))}
          <FormError error={update.error ?? remove.error} />
        </ul>
        <Card className="h-fit p-5">
          <h2 className="text-lg">Summary</h2>
          <div className="mt-3 flex justify-between text-sm"><span>Subtotal ({c.itemCount} items)</span><b>{rupees(c.subtotal)}</b></div>
          <p className="mt-1 text-xs text-ink-500">Delivery fee is added at checkout.</p>
          <Button className="mt-4 w-full" disabled={blocked} onClick={() => navigate('/checkout')}>Checkout</Button>
        </Card>
      </div>
    </div>
  )
}
