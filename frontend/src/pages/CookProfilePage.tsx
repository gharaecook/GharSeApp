import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDate } from '../lib/format'
import type { Batch, CookDetail, Dish, Page, Review } from '../lib/types'
import { BatchCard, DishCard } from '../components/cards'
import { AddToCart } from '../components/AddToCart'
import { Avatar, Badge, Button, EmptyState, ErrorState, LoadingState, Rating } from '../components/ui'

export default function CookProfilePage() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const qc = useQueryClient()
  const cook = useQuery({ queryKey: ['cook', id, user?.id], queryFn: () => api.get<CookDetail>(`/cooks/${id}`) })
  const dishes = useQuery({ queryKey: ['cook', id, 'dishes'], queryFn: () => api.get<Dish[]>(`/cooks/${id}/dishes`) })
  const batches = useQuery({ queryKey: ['cook', id, 'batches'], queryFn: () => api.get<Batch[]>(`/cooks/${id}/batches`) })
  const reviews = useQuery({ queryKey: ['cook', id, 'reviews'], queryFn: () => api.get<Page<Review>>(`/cooks/${id}/reviews?size=10`) })

  const fav = useMutation({
    mutationFn: (on: boolean) => (on ? api.post(`/cooks/${id}/favorite`) : api.del(`/cooks/${id}/favorite`)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cook', id] }); qc.invalidateQueries({ queryKey: ['favorites'] }) },
  })

  if (cook.isLoading) return <LoadingState />
  if (cook.error) return <ErrorState error={cook.error} onRetry={cook.refetch} />
  const d = cook.data!
  const c = d.summary
  const today = batches.data?.filter((b) => b.availableDate === new Date().toLocaleDateString('en-CA')) ?? []
  const availableDishIds = new Map<string, Batch[]>()
  batches.data?.forEach((b) => b.dishes.forEach((bd) => availableDishIds.set(bd.dishId, [...(availableDishIds.get(bd.dishId) ?? []), b])))

  return (
    <div className="container-page py-8">
      <section className="card overflow-hidden">
        <div className="bg-gradient-to-br from-brand-100 to-cream-200 p-6 sm:p-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <Avatar name={c.name} src={c.profilePhotoUrl} size={112} />
            <div className="flex-1">
              <p className="text-sm font-bold uppercase tracking-wider text-brand-600">Meet</p>
              <h1 className="text-4xl uppercase">{c.name.split(' ')[0]}</h1>
              <p className="text-ink-700">{[c.locality, c.city].filter(Boolean).join(', ')}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <Rating value={c.rating} count={c.reviewCount} size="lg" />
                {c.yearsOfCooking ? <Badge tone="brand">{c.yearsOfCooking}+ years cooking</Badge> : null}
                {d.kitchenVerified && <Badge tone="green">✓ Kitchen verified</Badge>}
                {d.fssaiVerified && <Badge tone="green">✓ FSSAI</Badge>}
                {c.availableToday && <Badge tone="amber">Available today</Badge>}
              </div>
            </div>
            {user?.role === 'CUSTOMER' && (
              <Button variant={d.favorite ? 'primary' : 'secondary'} onClick={() => fav.mutate(!d.favorite)} loading={fav.isPending} aria-pressed={!!d.favorite}>
                {d.favorite ? '♥ Favourited' : '♡ Favourite'}
              </Button>
            )}
          </div>
        </div>
        <div className="grid gap-6 p-6 sm:p-8 md:grid-cols-[2fr_1fr]">
          <div>
            {c.headline && <h2 className="text-xl text-brand-700">{c.headline}</h2>}
            {d.bio && <blockquote className="mt-2 border-l-4 border-brand-300 pl-4 text-lg italic text-ink-700">“{d.bio}”</blockquote>}
            {d.kitchenDescription && <p className="mt-3 text-sm text-ink-500"><b>The kitchen:</b> {d.kitchenDescription}</p>}
          </div>
          <div>
            {c.signatureDishes.length > 0 && <>
              <h3 className="mb-1 text-sm font-bold uppercase tracking-wide text-ink-500">Specialities</h3>
              <ul className="space-y-1 font-bold">{c.signatureDishes.map((s) => <li key={s}>• {s}</li>)}</ul>
            </>}
            <div className="mt-3 flex flex-wrap gap-1.5">{c.cuisines.map((x) => <Badge key={x}>{x}</Badge>)}</div>
          </div>
        </div>
        {d.kitchenPhotos.length > 0 && (
          <div className="flex gap-3 overflow-x-auto px-6 pb-6 sm:px-8">{d.kitchenPhotos.map((p) => <img key={p} src={p} alt="Kitchen" className="h-32 rounded-xl object-cover" />)}</div>
        )}
      </section>

      <section className="mt-10">
        <h2 className="mb-3 text-2xl">{today.length ? 'Available Today' : 'Available Food'}</h2>
        {batches.isLoading ? <LoadingState /> : !dishes.data?.length ? <EmptyState title="No dishes yet" message="This cook hasn't listed any food yet." /> : (
          <div className="grid gap-4 md:grid-cols-2">
            {dishes.data.map((dish) => {
              const open = (availableDishIds.get(dish.id) ?? []).filter((b) => b.orderingOpen)
              const batch = open[0]
              return (
                <DishCard key={dish.id} dish={dish} action={batch ? (
                  <div className="text-right">
                    <p className="mb-1 text-xs font-bold text-ink-500">Available: {batch.remainingQuantity} / {batch.capacity} · {formatDate(batch.availableDate, { weekday: 'short', day: 'numeric', month: 'short' })}</p>
                    <AddToCart dishId={dish.id} batchId={batch.id} max={batch.remainingQuantity} />
                  </div>
                ) : <Badge tone="red">{(availableDishIds.get(dish.id) ?? []).some((b) => b.soldOut) ? 'SOLD OUT' : 'Not in a batch'}</Badge>} />
              )
            })}
          </div>
        )}
      </section>

      <section className="mt-10">
        <h2 className="mb-3 text-2xl">Upcoming Batches</h2>
        {!batches.data?.length ? <EmptyState emoji="📅" title="No upcoming batches" message="Check back soon, or favourite this cook." /> : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {batches.data.map((b) => (
              <BatchCard key={b.id} batch={b}>
                <ul className="mt-3 space-y-2 border-t border-cream-200 pt-3">
                  {b.dishes.map((bd) => (
                    <li key={bd.dishId} className="flex items-center justify-between gap-2 text-sm">
                      <span><b>{bd.name}</b> · ₹{bd.price}</span>
                      {b.orderingOpen && <AddToCart dishId={bd.dishId} batchId={b.id} max={b.remainingQuantity} />}
                    </li>
                  ))}
                </ul>
              </BatchCard>
            ))}
          </div>
        )}
      </section>

      <section className="mt-10">
        <h2 className="mb-3 text-2xl">Reviews</h2>
        {reviews.isLoading ? <LoadingState /> : !reviews.data?.content.length ? <EmptyState emoji="💬" title="No reviews yet" message="Be the first to try and review." /> : (
          <div className="grid gap-3 md:grid-cols-2">
            {reviews.data.content.map((r) => (
              <article key={r.id} className="card p-4">
                <div className="flex items-center justify-between"><b>{r.customerName}</b><Rating value={r.rating} /></div>
                {r.comment && <p className="mt-1 text-sm text-ink-700">{r.comment}</p>}
                <p className="mt-2 text-xs text-ink-500">
                  {[['Taste', r.tasteRating], ['Hygiene', r.hygieneRating], ['Packaging', r.packagingRating], ['Reliability', r.reliabilityRating]].filter(([, v]) => v).map(([k, v]) => `${k} ${v}/5`).join(' · ')}
                </p>
              </article>
            ))}
          </div>
        )}
      </section>
      <p className="mt-8 text-center text-sm"><Link to="/cooks" className="font-bold text-brand-600 hover:underline">← Back to all Home Cooks</Link></p>
    </div>
  )
}
