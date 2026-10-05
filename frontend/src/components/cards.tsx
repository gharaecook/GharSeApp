import { Link } from 'react-router-dom'
import type { Batch, CookSummary, Dish, Order, OrderStatusValue } from '../lib/types'
import { categoryEmoji, formatDate, rupees, statusLabel, timeWindow, trackerSteps } from '../lib/format'
import { Avatar, Badge, Card, Rating } from './ui'

/** The cook is the hero: photo, name, place, story, speciality, rating, availability. */
export function CookCard({ cook }: { cook: CookSummary }) {
  return (
    <Card className="flex h-full flex-col overflow-hidden transition hover:-translate-y-0.5 hover:shadow-lg">
      <div className="flex items-center gap-4 bg-gradient-to-br from-brand-50 to-cream-200 p-4">
        <Avatar name={cook.name} src={cook.profilePhotoUrl} size={72} />
        <div className="min-w-0">
          <h3 className="truncate text-lg">{cook.name}</h3>
          <p className="truncate text-sm text-ink-500">{[cook.locality, cook.city].filter(Boolean).join(', ')}{cook.distanceKm !== undefined && ` · ${cook.distanceKm} km`}</p>
          <div className="mt-1 flex items-center gap-2"><Rating value={cook.rating} count={cook.reviewCount} />{cook.verified && <Badge tone="green">✓ Verified</Badge>}</div>
        </div>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-4">
        {cook.headline && <p className="font-bold text-brand-700">{cook.headline}</p>}
        {cook.storySnippet && <p className="line-clamp-2 text-sm text-ink-700">“{cook.storySnippet}”</p>}
        {cook.signatureDishes.length > 0 && (
          <p className="text-sm"><span className="font-bold text-ink-500">Speciality: </span>{cook.signatureDishes.slice(0, 3).join(' • ')}</p>
        )}
        <div className="flex flex-wrap gap-1.5">
          {cook.cuisines.slice(0, 3).map((c) => <Badge key={c}>{c}</Badge>)}
          {cook.availableToday && <Badge tone="brand">Available today</Badge>}
        </div>
        <Link to={`/cooks/${cook.id}`} className="mt-auto inline-flex justify-center rounded-xl bg-brand-500 px-4 py-2.5 text-sm font-bold text-white hover:bg-brand-600">
          View {cook.name.split(' ')[0]}
        </Link>
      </div>
    </Card>
  )
}

export function DishVisual({ dish, className = '' }: { dish: Pick<Dish, 'imageUrl' | 'category' | 'name'>; className?: string }) {
  if (dish.imageUrl) return <img src={dish.imageUrl} alt={dish.name} className={`object-cover ${className}`} />
  return <div aria-hidden className={`flex items-center justify-center bg-gradient-to-br from-cream-200 to-brand-100 text-4xl ${className}`}>{categoryEmoji[dish.category]}</div>
}

export function DishCard({ dish, action }: { dish: Dish; action?: React.ReactNode }) {
  return (
    <Card className="flex gap-3 p-3">
      <DishVisual dish={dish} className="h-24 w-24 shrink-0 rounded-xl" />
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-start justify-between gap-2">
          <Link to={`/dishes/${dish.id}`} className="font-extrabold hover:text-brand-600">{dish.name}</Link>
          <span className={`mt-1 h-3 w-3 shrink-0 rounded-sm border-2 ${dish.vegetarian ? 'border-leaf-600' : 'border-red-600'}`} title={dish.vegetarian ? 'Vegetarian' : 'Non-vegetarian'} />
        </div>
        {dish.description && <p className="line-clamp-2 text-sm text-ink-500">{dish.description}</p>}
        <div className="mt-auto flex items-center justify-between pt-2">
          <div><span className="font-extrabold">{rupees(dish.price)}</span>{dish.portionDescription && <span className="ml-2 text-xs text-ink-500">{dish.portionDescription}</span>}</div>
          {action}
        </div>
      </div>
    </Card>
  )
}

/** Remaining/capacity as a bar so scarcity is obvious. */
export function BatchCard({ batch, children, showCook }: { batch: Batch; children?: React.ReactNode; showCook?: boolean }) {
  const pct = Math.round((batch.reservedQuantity / batch.capacity) * 100)
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          {showCook && <Link to={`/cooks/${batch.cookId}`} className="text-xs font-bold uppercase tracking-wide text-brand-600">{batch.cookName} · {batch.cookLocality}</Link>}
          <h3 className="text-lg">{batch.name}</h3>
          <p className="text-sm font-semibold text-ink-700">{formatDate(batch.availableDate)}</p>
        </div>
        {batch.soldOut ? <Badge tone="red">SOLD OUT</Badge> : !batch.orderingOpen ? <Badge>Closed</Badge> : batch.remainingQuantity <= 5 ? <Badge tone="amber">Only {batch.remainingQuantity} left</Badge> : <Badge tone="green">Open</Badge>}
      </div>
      {batch.description && <p className="mt-1 text-sm text-ink-500">{batch.description}</p>}
      <div className="mt-3">
        <div className="mb-1 flex justify-between text-xs font-bold text-ink-500"><span>Available: {batch.remainingQuantity} / {batch.capacity}</span><span>{pct}% booked</span></div>
        <div className="h-2 overflow-hidden rounded-full bg-cream-200"><div className={`h-full rounded-full ${batch.soldOut ? 'bg-red-500' : 'bg-leaf-500'}`} style={{ width: `${pct}%` }} /></div>
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-2 text-xs text-ink-700">
        {batch.pickupStart && <div><dt className="font-bold text-ink-500">Pickup</dt><dd>{timeWindow(batch.pickupStart, batch.pickupEnd)}</dd></div>}
        {batch.deliveryStart && <div><dt className="font-bold text-ink-500">Delivery</dt><dd>{timeWindow(batch.deliveryStart, batch.deliveryEnd)}</dd></div>}
        <div className="col-span-2"><dt className="font-bold text-ink-500">Pre-order closes</dt><dd>{new Date(batch.orderCutoff).toLocaleString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })}</dd></div>
      </dl>
      {children}
    </Card>
  )
}

/** Step tracker: ✓ done, ● current, ○ upcoming. */
export function OrderTracker({ order }: { order: Pick<Order, 'orderStatus' | 'orderType'> }) {
  if (order.orderStatus === 'CANCELLED') return <Badge tone="red">✕ Order cancelled</Badge>
  const steps = trackerSteps(order.orderType)
  const current = steps.indexOf(order.orderStatus as OrderStatusValue)
  return (
    <ol className="space-y-2" aria-label="Order progress">
      {steps.map((s, i) => (
        <li key={s} className={`flex items-center gap-3 text-sm ${i <= current ? 'font-bold text-ink-900' : 'text-ink-300'}`}>
          <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs ${i < current ? 'bg-leaf-600 text-white' : i === current ? 'bg-brand-500 text-white' : 'border border-cream-300'}`}>
            {i < current ? '✓' : i === current ? '●' : '○'}
          </span>
          {statusLabel[s]}
        </li>
      ))}
    </ol>
  )
}

export function OrderStatusBadge({ status }: { status: OrderStatusValue }) {
  const tone = status === 'COMPLETED' ? 'green' : status === 'CANCELLED' ? 'red' : status === 'CREATED' ? 'amber' : 'brand'
  return <Badge tone={tone}>{statusLabel[status]}</Badge>
}
