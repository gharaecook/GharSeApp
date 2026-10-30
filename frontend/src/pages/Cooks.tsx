import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, qs } from '../lib/api'
import { categoryLabel } from '../lib/format'
import { getCurrentLocation } from '../lib/deviceLocation'
import type { CookSummary, Cuisine, DishCategory, Page } from '../lib/types'
import { CookCard } from '../components/cards'
import { Button, EmptyState, ErrorState, Input, LoadingState, PageTitle, Select } from '../components/ui'
import { GooglePlaceMap, type SelectedPlace } from '../components/GooglePlaceMap'
import { useState } from 'react'

export default function Cooks() {
  const [params, setParams] = useSearchParams()
  const [selectedPlace, setSelectedPlace] = useState<SelectedPlace | null>(() => {
    const rawLatitude = params.get('lat')
    const rawLongitude = params.get('lng')
    const latitude = Number(rawLatitude)
    const longitude = Number(rawLongitude)
    const locality = params.get('locality') ?? ''
    const city = params.get('city') ?? ''
    const name = locality || city
    if (!name || rawLatitude === null || rawLongitude === null || !Number.isFinite(latitude) || !Number.isFinite(longitude)) return null
    return { placeId: 'search-location', name, formattedAddress: name, locality, city, latitude, longitude }
  })
  const [geoError, setGeoError] = useState('')
  const get = (k: string) => params.get(k) ?? ''
  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params)
    if (v) next.set(k, v); else next.delete(k)
    next.delete('page')
    setParams(next)
  }
  const page = Number(get('page') || 0)
  const setLocality = (locality: string) => {
    setSelectedPlace(null)
    const next = new URLSearchParams(params)
    if (locality) next.set('locality', locality); else next.delete('locality')
    next.delete('city')
    next.delete('lat')
    next.delete('lng')
    next.delete('page')
    setParams(next)
  }
  const choosePlace = (place: SelectedPlace) => {
    setSelectedPlace(place)
    const next = new URLSearchParams(params)
    if (place.locality) next.set('locality', place.locality); else next.delete('locality')
    if (place.city) next.set('city', place.city); else next.delete('city')
    next.set('lat', String(place.latitude))
    next.set('lng', String(place.longitude))
    next.delete('page')
    setParams(next)
  }

  const cuisines = useQuery({ queryKey: ['cuisines'], queryFn: () => api.get<Cuisine[]>('/cuisines') })
  const query = {
    q: get('q'), cuisine: get('cuisine'), category: get('category'), vegetarian: get('vegetarian') || undefined,
    minRating: get('minRating'), availableToday: get('availableToday') || undefined, locality: get('locality'), city: get('city'),
    sort: get('sort') || undefined, page, size: 9, lat: selectedPlace?.latitude, lng: selectedPlace?.longitude,
  }
  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ['cooks', 'search', query],
    queryFn: () => api.get<Page<CookSummary>>(`/cooks${qs(query)}`),
    placeholderData: (prev) => prev,
  })
  const topCooks = useQuery({
    queryKey: ['cooks', 'top'],
    queryFn: () => api.get<CookSummary[]>('/cooks/top'),
  })
  const topRanks = new Map((topCooks.data ?? []).map((cook, index) => [cook.id, index + 1]))

  const locateMe = async () => {
    setGeoError('')
    try {
      const position = await getCurrentLocation()
      setSelectedPlace({
        placeId: 'current-location',
        name: 'Your location',
        formattedAddress: 'Current location',
        locality: '',
        city: '',
        latitude: position.latitude,
        longitude: position.longitude,
      })
      const next = new URLSearchParams(params)
      next.delete('city')
      next.delete('locality')
      next.delete('lat')
      next.delete('lng')
      setParams(next)
    } catch {
      setGeoError('Could not get your location. You can still filter by locality.')
    }
  }

  return (
    <div className="container-page py-8">
      <PageTitle title="Home Cooks" subtitle={data ? `${data.totalElements} cook${data.totalElements === 1 ? '' : 's'} found` : undefined} />
      {!get('city') && !get('locality') && (topCooks.isLoading ? <LoadingState label="Finding top cooks…" /> : topCooks.error ? <ErrorState error={topCooks.error} onRetry={topCooks.refetch} /> : !!topCooks.data?.length && (
        <section className="mb-8" aria-labelledby="top-cooks-title">
          <div className="mb-4">
            <h2 id="top-cooks-title" className="text-2xl">Top Home Cooks</h2>
            <p className="text-sm text-ink-500">Ranked by completed orders, with reviews as the tie-breaker.</p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {topCooks.data.map((cook, index) => <CookCard key={cook.id} cook={cook} awardRank={index + 1} />)}
          </div>
        </section>
      ))}
      <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
        <aside className="card h-fit space-y-4 p-4" aria-label="Filters">
          <Input label="Search" placeholder="Cook, dish, cuisine…" defaultValue={get('q')} key={get('q')}
            onKeyDown={(e) => e.key === 'Enter' && set('q', e.currentTarget.value)} onBlur={(e) => e.currentTarget.value !== get('q') && set('q', e.currentTarget.value)} />
          <GooglePlaceMap value={selectedPlace} onSelect={choosePlace} mapHeight="h-56" />
          <Input label="Locality (manual search)" placeholder="e.g. Bavdhan Pune" defaultValue={get('locality')} key={`l-${get('locality')}`}
            onKeyDown={(e) => e.key === 'Enter' && setLocality(e.currentTarget.value)} onBlur={(e) => e.currentTarget.value !== get('locality') && setLocality(e.currentTarget.value)} />
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
            <Button variant="secondary" className="w-full" onClick={selectedPlace ? () => {
              setSelectedPlace(null)
              const next = new URLSearchParams(params)
              next.delete('lat')
              next.delete('lng')
              setParams(next)
            } : locateMe}>{selectedPlace ? '✓ Location selected (clear)' : '📍 Sort by distance'}</Button>
            {geoError && <p className="mt-1 text-xs text-red-600">{geoError}</p>}
          </div>
          <Button variant="ghost" className="w-full" onClick={() => { setParams({}); setSelectedPlace(null) }}>Clear filters</Button>
        </aside>

        <div aria-busy={isFetching}>
          {isLoading ? <LoadingState /> : error ? <ErrorState error={error} onRetry={refetch} /> : !data?.content.length ? (
            <EmptyState title="No cooks match yet" message="Try removing a filter or searching for something else." action={<Button variant="secondary" onClick={() => setParams({})}>Clear filters</Button>} />
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{data.content.map((c) => <CookCard key={c.id} cook={c} awardRank={topRanks.get(c.id)} />)}</div>
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
