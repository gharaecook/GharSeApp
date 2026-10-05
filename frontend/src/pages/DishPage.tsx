import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { categoryLabel, formatDate, rupees } from '../lib/format'
import type { Batch, Dish } from '../lib/types'
import { DishVisual } from '../components/cards'
import { AddToCart } from '../components/AddToCart'
import { Badge, ErrorState, LoadingState } from '../components/ui'

export default function DishPage() {
  const { id = '' } = useParams()
  const dish = useQuery({ queryKey: ['dish', id], queryFn: () => api.get<Dish>(`/dishes/${id}`) })
  const batches = useQuery({
    queryKey: ['cook', dish.data?.cookId, 'batches'], enabled: !!dish.data,
    queryFn: () => api.get<Batch[]>(`/cooks/${dish.data!.cookId}/batches`),
  })
  if (dish.isLoading) return <LoadingState />
  if (dish.error) return <ErrorState error={dish.error} onRetry={dish.refetch} />
  const d = dish.data!
  const offers = batches.data?.filter((b) => b.dishes.some((x) => x.dishId === d.id)) ?? []

  return (
    <div className="container-page py-8">
      <div className="card grid gap-6 overflow-hidden md:grid-cols-2">
        <DishVisual dish={d} className="h-64 w-full md:h-full" />
        <div className="p-6">
          <p className="text-sm font-bold text-brand-600">by <Link className="hover:underline" to={`/cooks/${d.cookId}`}>{d.cookName}</Link></p>
          <h1 className="mt-1 text-3xl">{d.name}</h1>
          <div className="mt-2 flex flex-wrap gap-2">
            <Badge tone={d.vegetarian ? 'green' : 'red'}>{d.vegetarian ? 'Vegetarian' : 'Non-vegetarian'}</Badge>
            <Badge>{categoryLabel[d.category]}</Badge>
            {d.cuisine && <Badge>{d.cuisine}</Badge>}
            {d.preparationTimeMinutes ? <Badge>{d.preparationTimeMinutes} min prep</Badge> : null}
          </div>
          <p className="mt-3 text-2xl font-extrabold">{rupees(d.price)} <span className="text-sm font-semibold text-ink-500">{d.portionDescription}</span></p>
          {d.description && <p className="mt-3 text-ink-700">{d.description}</p>}
          {d.ingredients && <p className="mt-3 text-sm"><b>Ingredients:</b> {d.ingredients}</p>}
          {d.allergens && <p className="mt-1 text-sm"><b>Allergens:</b> {d.allergens}</p>}
        </div>
      </div>

      <h2 className="mb-3 mt-8 text-2xl">When can you get it?</h2>
      {batches.isLoading ? <LoadingState /> : !offers.length ? <p className="text-ink-500">No batches scheduled right now. <Link className="font-bold text-brand-600" to={`/cooks/${d.cookId}`}>See {d.cookName}'s kitchen</Link>.</p> : (
        <ul className="space-y-3">
          {offers.map((b) => (
            <li key={b.id} className="card flex flex-wrap items-center justify-between gap-3 p-4">
              <div>
                <p className="font-extrabold">{b.name} · {formatDate(b.availableDate)}</p>
                <p className="text-sm text-ink-500">{b.soldOut ? 'SOLD OUT' : `Available: ${b.remainingQuantity} / ${b.capacity}`}</p>
              </div>
              {b.orderingOpen ? <AddToCart dishId={d.id} batchId={b.id} max={b.remainingQuantity} /> : <Badge tone="red">{b.soldOut ? 'SOLD OUT' : 'Closed'}</Badge>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
