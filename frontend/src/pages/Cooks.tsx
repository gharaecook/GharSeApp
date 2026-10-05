import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, qs } from '../lib/api'
import { categoryLabel } from '../lib/format'
import type { CookSummary, Cuisine, DishCategory, Page } from '../lib/types'
import { CookCard } from '../components/cards'
import { Button, EmptyState, ErrorState, Input, LoadingState, PageTitle, Select } from '../components/ui'
import { useState } from 'react'

export default function Cooks() {
  const [params, setParams] = useSearchParams()
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null)
  const [geoError, setGeoError] = useState('')
  const get = (k: string) => params.get(k) ?? ''
  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params)
    if (v) next.set(k, v); else next.delete(k)
    next.delete('page')
    setParams(next)
  }
  const page = Number(get('page') || 0)

  const cuisines = useQuery({ queryKey: ['cuisines'], queryFn: () => api.get<Cuisine[]>('/cuisines') })
  const query = {
    q: get('q'), cuisine: get('cuisine'), category: get('category'), vegetarian: get('vegetarian') || undefined,
    minRating: get('minRating'), availableToday: get('availableToday') || undefined, locality: get('locality'),
    sort: get('sort') || undefined, page, size: 9, lat: coords?.lat, lng: coords?.lng,
  }
  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ['cooks', 'search', query],
    queryFn: () => api.get<Page<CookSummary>>(`/cooks${qs(query)}`),
    placeholderData: (prev) => prev,
  })

  const locateMe = () => {
    setGeoError('')
    navigator.geolocation?.getCurrentPosition(
      (p) => setCoords({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => setGeoError('Could not get your location. You can still filter by locality.'),
    )
    if (!navigator.geolocation) setGeoError('Location is not supported in this browser.')
  }

  return (
    <div className="container-page py-8">
      <PageTitle title="Home Cooks" subtitle={data ? `${data.totalElements} cook${data.totalElements === 1 ? '' : 's'} found` : undefined} />
      <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
        <aside className="card h-fit space-y-4 p-4" aria-label="Filters">
          <Input label="Search" placeholder="Cook, dish, cuisine…" defaultValue={get('q')} key={get('q')}
            onKeyDown={(e) => e.key === 'Enter' && set('q', e.currentTarget.value)} onBlur={(e) => e.currentTarget.value !== get('q') && set('q', e.currentTarget.value)} />
          <Input label="Locality" placeholder="e.g. Kothrud" defaultValue={get('locality')} key={`l-${get('locality')}`}
            onKeyDown={(e) => e.key === 'Enter' && set('locality', e.currentTarget.value)} onBlur={(e) => e.currentTarget.value !== get('locality') && set('locality', e.currentTarget.value)} />
          <Select label="Cuisine" value={get('cuisine')} onChange={(e) => set('cuisine', e.target.value)}>
            <option value="">Any</option>
            {cuisines.data?.map((c) => <option key={c.id}>{c.name}</option>)}
          </Select>
          <Select label="Dish category" value={get('category')} onChange={(e) => set('category', e.target.value)}>
            <option value="">Any</option>
            {(Object.keys(categoryLabel) as DishCategory[]).map((c) => <option key={c} value={c}>{categoryLabel[c]}</option>)}
          </Select>
          <Select label="Minimum rating" value={get('minRating')} onChange={(e) => set('minRating', e.target.value)}>
            <option value="">Any</option><option value="3">3★ & up</option><option value="4">4★ & up</option><option value="4.5">4.5★ & up</option>
          </Select>
          <Select label="Sort by" value={get('sort')} onChange={(e) => set('sort', e.target.value)}>
            <option value="">Top rated</option><option value="popular">Most popular</option><option value="newest">Newest</option>
          </Select>
          <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={get('vegetarian') === 'true'} onChange={(e) => set('vegetarian', e.target.checked ? 'true' : '')} /> Vegetarian options</label>
          <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={get('availableToday') === 'true'} onChange={(e) => set('availableToday', e.target.checked ? 'true' : '')} /> Available today</label>
          <div>
            <Button variant="secondary" className="w-full" onClick={coords ? () => setCoords(null) : locateMe}>{coords ? '✓ Nearest first (clear)' : '📍 Sort by distance'}</Button>
            {geoError && <p className="mt-1 text-xs text-red-600">{geoError}</p>}
          </div>
          <Button variant="ghost" className="w-full" onClick={() => setParams({})}>Clear filters</Button>
        </aside>

        <div aria-busy={isFetching}>
          {isLoading ? <LoadingState /> : error ? <ErrorState error={error} onRetry={refetch} /> : !data?.content.length ? (
            <EmptyState title="No cooks match yet" message="Try removing a filter or searching for something else." action={<Button variant="secondary" onClick={() => setParams({})}>Clear filters</Button>} />
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{data.content.map((c) => <CookCard key={c.id} cook={c} />)}</div>
              {data.totalPages > 1 && (
                <div className="mt-6 flex items-center justify-center gap-3">
                  <Button variant="secondary" disabled={page === 0} onClick={() => set('page', String(page - 1))}>← Previous</Button>
                  <span className="text-sm font-semibold">Page {page + 1} of {data.totalPages}</span>
                  <Button variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => set('page', String(page + 1))}>Next →</Button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
