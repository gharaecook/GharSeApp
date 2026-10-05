import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import type { Batch, Dish } from '../../lib/types'
import { CookShell } from '../../components/SectionNav'
import { BatchCard } from '../../components/cards'
import { Button, Card, EmptyState, ErrorState, FormError, Input, LoadingState, PageTitle, Select, Textarea } from '../../components/ui'

export function CookBatches() {
  const batches = useQuery({ queryKey: ['cook', 'batches'], queryFn: () => api.get<Batch[]>('/cook/batches') })
  return (
    <CookShell>
      <PageTitle title="Batches" subtitle="Limited runs of food for a given day. Customers pre-order until the cut-off."
        action={<Link to="/cook/batches/new" className="rounded-xl bg-brand-500 px-4 py-2.5 text-sm font-bold text-white">+ New batch</Link>} />
      {batches.isLoading ? <LoadingState /> : batches.error ? <ErrorState error={batches.error} onRetry={batches.refetch} /> : !batches.data?.length ? (
        <EmptyState emoji="📅" title="No batches yet" message="Create a batch, e.g. “Sunday Special: 30 portions of Puran Poli”." action={<Link to="/cook/batches/new" className="font-bold text-brand-600">Create a batch</Link>} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {batches.data.map((b) => (
            <BatchCard key={b.id} batch={b}>
              <p className="mt-3 text-sm text-ink-700">{b.dishes.map((d) => `${d.name} ₹${d.price}`).join(' · ')}</p>
              <p className="mt-2 text-xs font-bold text-ink-500">Status: {b.status}</p>
              <Link to={`/cook/batches/${b.id}`} className="mt-2 inline-block text-sm font-bold text-brand-600">Edit</Link>
            </BatchCard>
          ))}
        </div>
      )}
    </CookShell>
  )
}

/** ISO instant -> value for <input type="datetime-local"> in the browser's zone. */
const toLocalInput = (iso: string) => {
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}
const hhmm = (t?: string) => (t ? t.slice(0, 5) : '')

export function BatchForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const dishes = useQuery({ queryKey: ['cook', 'dishes'], queryFn: () => api.get<Dish[]>('/cook/dishes') })
  const batches = useQuery({ queryKey: ['cook', 'batches'], queryFn: () => api.get<Batch[]>('/cook/batches'), enabled: !!id })
  const existing = id ? batches.data?.find((b) => b.id === id) : undefined
  if (dishes.isLoading || (id && batches.isLoading)) return <CookShell><LoadingState /></CookShell>
  if (dishes.error) return <CookShell><ErrorState error={dishes.error} onRetry={dishes.refetch} /></CookShell>
  if (id && !existing) return <CookShell><ErrorState error={new Error('Batch not found')} /></CookShell>
  return <BatchFormInner key={existing?.id ?? 'new'} existing={existing} dishes={dishes.data!.filter((d) => d.active)} onSaved={() => { qc.invalidateQueries({ queryKey: ['cook', 'batches'] }); navigate('/cook/batches') }} />
}

function BatchFormInner({ existing, dishes, onSaved }: { existing?: Batch; dishes: Dish[]; onSaved: () => void }) {
  const navigate = useNavigate()
  const tomorrow = new Date(Date.now() + 86_400_000).toLocaleDateString('en-CA')
  const [f, setF] = useState({
    name: existing?.name ?? '', description: existing?.description ?? '', availableDate: existing?.availableDate ?? tomorrow,
    orderCutoff: existing ? toLocalInput(existing.orderCutoff) : `${new Date().toLocaleDateString('en-CA')}T20:00`,
    capacity: existing?.capacity ?? 20, pickupStart: hhmm(existing?.pickupStart) || '12:00', pickupEnd: hhmm(existing?.pickupEnd) || '14:00',
    deliveryStart: hhmm(existing?.deliveryStart) || '12:00', deliveryEnd: hhmm(existing?.deliveryEnd) || '15:00',
    offerPickup: existing ? !!existing.pickupStart : true, offerDelivery: existing ? !!existing.deliveryStart : true, status: existing?.status ?? 'OPEN',
  })
  const [chosen, setChosen] = useState<Record<string, string>>(Object.fromEntries(existing?.dishes.map((d) => [d.dishId, String(d.price)]) ?? []))
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((s) => ({ ...s, [k]: v }))

  const save = useMutation({
    mutationFn: () => {
      const body = {
        name: f.name, description: f.description || undefined, availableDate: f.availableDate,
        orderCutoff: new Date(f.orderCutoff).toISOString(), capacity: Number(f.capacity),
        pickupStart: f.offerPickup ? f.pickupStart : undefined, pickupEnd: f.offerPickup ? f.pickupEnd : undefined,
        deliveryStart: f.offerDelivery ? f.deliveryStart : undefined, deliveryEnd: f.offerDelivery ? f.deliveryEnd : undefined,
        status: f.status, dishes: Object.entries(chosen).map(([dishId, price]) => ({ dishId, price: price === '' ? undefined : Number(price) })),
      }
      return existing ? api.put(`/cook/batches/${existing.id}`, body) : api.post('/cook/batches', body)
    },
    onSuccess: onSaved,
  })
  const valid = f.name.trim() && Number(f.capacity) > 0 && Object.keys(chosen).length > 0 && f.orderCutoff && (f.offerPickup || f.offerDelivery)

  return (
    <CookShell>
      <PageTitle title={existing ? 'Edit batch' : 'New batch'} subtitle={existing ? `${existing.reservedQuantity} of ${existing.capacity} portions already reserved` : undefined} />
      <Card className="max-w-2xl p-5">
        <form onSubmit={(e) => { e.preventDefault(); save.mutate() }} className="space-y-4">
          <Input label="Batch name" placeholder="Sunday Special" value={f.name} onChange={(e) => set('name', e.target.value)} />
          <Textarea label="Description" rows={2} value={f.description} onChange={(e) => set('description', e.target.value)} />
          <div className="grid gap-4 sm:grid-cols-3">
            <Input label="Date" type="date" value={f.availableDate} onChange={(e) => set('availableDate', e.target.value)} />
            <Input label="Pre-order closes" type="datetime-local" value={f.orderCutoff} onChange={(e) => set('orderCutoff', e.target.value)} />
            <Input label="Total portions" type="number" min={1} value={f.capacity} onChange={(e) => set('capacity', Number(e.target.value))} hint="Shared across the dishes below" />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <fieldset className="space-y-2 rounded-xl border border-cream-300 p-3">
              <label className="flex items-center gap-2 text-sm font-bold"><input type="checkbox" checked={f.offerPickup} onChange={(e) => set('offerPickup', e.target.checked)} /> Customer pickup</label>
              {f.offerPickup && <div className="grid grid-cols-2 gap-2"><Input aria-label="Pickup from" type="time" value={f.pickupStart} onChange={(e) => set('pickupStart', e.target.value)} /><Input aria-label="Pickup until" type="time" value={f.pickupEnd} onChange={(e) => set('pickupEnd', e.target.value)} /></div>}
            </fieldset>
            <fieldset className="space-y-2 rounded-xl border border-cream-300 p-3">
              <label className="flex items-center gap-2 text-sm font-bold"><input type="checkbox" checked={f.offerDelivery} onChange={(e) => set('offerDelivery', e.target.checked)} /> Delivery</label>
              {f.offerDelivery && <div className="grid grid-cols-2 gap-2"><Input aria-label="Delivery from" type="time" value={f.deliveryStart} onChange={(e) => set('deliveryStart', e.target.value)} /><Input aria-label="Delivery until" type="time" value={f.deliveryEnd} onChange={(e) => set('deliveryEnd', e.target.value)} /></div>}
            </fieldset>
          </div>
          <fieldset>
            <legend className="label">Dishes in this batch</legend>
            {!dishes.length ? <p className="text-sm text-ink-500">Add an active dish first.</p> : (
              <ul className="space-y-2">
                {dishes.map((d) => (
                  <li key={d.id} className="flex items-center justify-between gap-3 rounded-xl border border-cream-300 p-2">
                    <label className="flex items-center gap-2 text-sm font-bold"><input type="checkbox" checked={d.id in chosen} onChange={(e) => setChosen((c) => { const n = { ...c }; if (e.target.checked) n[d.id] = String(d.price); else delete n[d.id]; return n })} />{d.name}</label>
                    {d.id in chosen && <label className="flex items-center gap-1 text-sm">₹<input aria-label={`Price for ${d.name}`} type="number" min={0} step="0.5" className="input !w-24 !py-1" value={chosen[d.id]} onChange={(e) => setChosen((c) => ({ ...c, [d.id]: e.target.value }))} /></label>}
                  </li>
                ))}
              </ul>
            )}
          </fieldset>
          {existing && <Select label="Status" value={f.status} onChange={(e) => set('status', e.target.value as Batch['status'])}><option value="OPEN">Open</option><option value="CLOSED">Closed (stop orders)</option><option value="CANCELLED">Cancelled</option></Select>}
          <FormError error={save.error} />
          <div className="flex gap-2"><Button type="submit" disabled={!valid} loading={save.isPending}>Save batch</Button><Button type="button" variant="ghost" onClick={() => navigate('/cook/batches')}>Cancel</Button></div>
        </form>
      </Card>
    </CookShell>
  )
}
