import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { formatDate, formatDateTime, orderTypeLabel, rupees, timeWindow } from '../../lib/format'
import type { Earnings, Order, OrderStatusValue, Page } from '../../lib/types'
import { CookShell } from '../../components/SectionNav'
import { OrderStatusBadge } from '../../components/cards'
import { Button, Card, EmptyState, ErrorState, FormError, LoadingState, PageTitle } from '../../components/ui'

/** The single next action the cook can take for an order in each status. */
function nextAction(o: Order): { label: string; path: string } | null {
  switch (o.orderStatus) {
    case 'CREATED': return { label: 'Confirm order', path: 'confirm' }
    case 'CONFIRMED': return { label: 'Start preparing', path: 'prepare' }
    case 'PREPARING': return { label: 'Mark ready', path: 'ready' }
    case 'READY': return { label: o.orderType === 'PICKUP' ? 'Ready for pickup' : 'Out for delivery', path: 'dispatch' }
    case 'READY_FOR_PICKUP': return { label: 'Customer picked up', path: 'handover' }
    case 'OUT_FOR_DELIVERY': return { label: 'Mark delivered', path: 'handover' }
    case 'PICKED_UP': case 'DELIVERED': return { label: 'Complete order', path: 'complete' }
    default: return null
  }
}

const FILTERS: { label: string; statuses?: OrderStatusValue[] }[] = [
  { label: 'Open', statuses: ['CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'PICKED_UP', 'DELIVERED'] },
  { label: 'Completed', statuses: ['COMPLETED'] },
  { label: 'Cancelled', statuses: ['CANCELLED'] },
  { label: 'All' },
]

export function CookOrders() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState(0)
  const statuses = FILTERS[filter].statuses
  const orders = useQuery({
    queryKey: ['cook', 'orders', filter],
    queryFn: () => api.get<Page<Order>>(`/cook/orders?size=50${statuses ? `&status=${statuses.join(',')}` : ''}`),
    refetchInterval: 20_000,
  })
  const advance = useMutation({
    mutationFn: (v: { id: string; path: string }) => api.post<Order>(`/cook/orders/${v.id}/${v.path}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cook'] }) },
  })
  return (
    <CookShell>
      <PageTitle title="Orders" />
      <div className="mb-4 flex gap-2" role="tablist">
        {FILTERS.map((f, i) => <button key={f.label} role="tab" aria-selected={filter === i} onClick={() => setFilter(i)} className={`rounded-full px-4 py-1.5 text-sm font-bold ${filter === i ? 'bg-brand-500 text-white' : 'bg-white text-ink-700'}`}>{f.label}</button>)}
      </div>
      <FormError error={advance.error} />
      {orders.isLoading ? <LoadingState /> : orders.error ? <ErrorState error={orders.error} onRetry={orders.refetch} /> : !orders.data?.content.length ? <EmptyState emoji="🧾" title="No orders here" message="New orders from customers will appear here." /> : (
        <ul className="mt-3 space-y-3">
          {orders.data.content.map((o) => {
            const action = nextAction(o)
            return (
              <li key={o.id}>
                <Card className="p-4">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-extrabold">#{o.orderNumber} · {o.customerName}</p>
                      <p className="text-xs text-ink-500">{formatDateTime(o.createdAt)}{o.customerPhone && ` · ${o.customerPhone}`}</p>
                    </div>
                    <div className="text-right"><OrderStatusBadge status={o.orderStatus} /><p className="mt-1 font-extrabold">{rupees(o.totalAmount)}</p></div>
                  </div>
                  <ul className="mt-2 text-sm">
                    {o.items.map((i, idx) => <li key={idx}>{i.quantity} × {i.dishName} <span className="text-ink-500">— {formatDate(i.availableDate, { weekday: 'short', day: 'numeric', month: 'short' })}, {o.orderType === 'PICKUP' ? timeWindow(i.pickupStart, i.pickupEnd) : timeWindow(i.deliveryStart, i.deliveryEnd)}</span></li>)}
                  </ul>
                  <p className="mt-2 text-sm text-ink-700"><b>{orderTypeLabel[o.orderType]}</b>{o.deliveryAddress && ` · ${o.deliveryAddress}`}</p>
                  {action && <Button className="mt-3" loading={advance.isPending && advance.variables?.id === o.id} onClick={() => advance.mutate({ id: o.id, path: action.path })}>{action.label}</Button>}
                </Card>
              </li>
            )
          })}
        </ul>
      )}
    </CookShell>
  )
}

export function CookEarnings() {
  const e = useQuery({ queryKey: ['cook', 'earnings'], queryFn: () => api.get<Earnings>('/cook/earnings') })
  if (e.isLoading) return <CookShell><LoadingState /></CookShell>
  if (e.error) return <CookShell><ErrorState error={e.error} onRetry={e.refetch} /></CookShell>
  const d = e.data!
  const stats = [['Today', d.earningsToday], ['This month', d.earningsThisMonth], ['Total earned', d.totalEarnings], ['In progress', d.pendingEarnings]] as const
  return (
    <CookShell>
      <PageTitle title="Earnings" subtitle={`${d.completedOrders} completed order${d.completedOrders === 1 ? '' : 's'}`} />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map(([label, v]) => <Card key={label} className="p-4"><p className="text-xs font-bold uppercase tracking-wide text-ink-500">{label}</p><p className="mt-1 text-2xl font-extrabold">{rupees(v)}</p></Card>)}
      </div>
      <h2 className="mb-2 mt-8 text-lg">Recently completed</h2>
      {!d.recent.length ? <EmptyState title="Nothing completed yet" /> : (
        <Card className="divide-y divide-cream-200">
          {d.recent.map((r) => <div key={r.orderId} className="flex justify-between p-3 text-sm"><span>#{r.orderNumber} <span className="text-ink-500">· {formatDateTime(r.completedAt)}</span></span><b>{rupees(r.amount)}</b></div>)}
        </Card>
      )}
    </CookShell>
  )
}
