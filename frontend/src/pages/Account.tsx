import { lazy, Suspense, useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { CookSummary, UserProfile } from '../lib/types'
import { CookCard } from '../components/cards'
import { Button, Card, EmptyState, ErrorState, FormError, Input, LoadingState, PageTitle } from '../components/ui'

const KitchenLocationPicker = lazy(() => import('../components/KitchenLocationPicker')
  .then(({ KitchenLocationPicker: component }) => ({ default: component })))

type ProfileForm = {
  name: string
  phone: string
  address: string
  society: string
  area: string
  landmark: string
  deliveryLatitude: number | ''
  deliveryLongitude: number | ''
}

export function Profile() {
  const qc = useQueryClient()
  const me = useQuery({ queryKey: ['me'], queryFn: () => api.get<UserProfile>('/users/me') })
  const [locationError, setLocationError] = useState('')
  const [locating, setLocating] = useState(false)
  const { register, handleSubmit, setValue, watch } = useForm<ProfileForm>({
    values: me.data && {
      name: me.data.name,
      phone: me.data.phone,
      address: me.data.address ?? '',
      society: me.data.society ?? '',
      area: me.data.area ?? '',
      landmark: me.data.landmark ?? '',
      deliveryLatitude: me.data.deliveryLatitude ?? '',
      deliveryLongitude: me.data.deliveryLongitude ?? '',
    },
  })
  const latitude = watch('deliveryLatitude')
  const longitude = watch('deliveryLongitude')
  const save = useMutation({
    mutationFn: (v: ProfileForm) => api.put<UserProfile>('/users/me', {
      ...v,
      deliveryLatitude: v.deliveryLatitude === '' ? undefined : v.deliveryLatitude,
      deliveryLongitude: v.deliveryLongitude === '' ? undefined : v.deliveryLongitude,
    }),
    onSuccess: (u) => qc.setQueryData(['me'], u),
  })
  const setDeliveryLocation = (lat: number, lng: number) => {
    setValue('deliveryLatitude', lat, { shouldDirty: true })
    setValue('deliveryLongitude', lng, { shouldDirty: true })
    setLocationError('')
  }
  const useCurrentLocation = () => {
    setLocationError('')
    if (!navigator.geolocation) {
      setLocationError('Location is not supported in this browser. Select a point on the map instead.')
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setDeliveryLocation(coords.latitude, coords.longitude)
        setLocating(false)
      },
      (error) => {
        setLocationError(error.code === error.PERMISSION_DENIED
          ? 'Location permission was denied. Allow it in your browser or select a point on the map.'
          : 'Could not get your location. Try again or select a point on the map.')
        setLocating(false)
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
    )
  }
  if (me.isLoading) return <LoadingState />
  if (me.error) return <ErrorState error={me.error} onRetry={me.refetch} />
  return (
    <div className="container-page max-w-2xl py-8">
      <PageTitle title="Your profile" subtitle={me.data!.email} />
      <Card className="p-6">
        <form onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-4">
          <Input label="Name" {...register('name', { required: true })} />
          <Input label="Mobile" type="tel" {...register('phone', { required: true })} />
          <h2 className="pt-2 text-lg">Home delivery address</h2>
          <p className="text-sm text-ink-500">Add your flat, society, and area so your order can reach the right place.</p>
          <Input label="Flat / house / street" maxLength={500} placeholder="Flat 12, Building A" {...register('address')} />
          <Input label="Society / building" maxLength={120} placeholder="Society or apartment name" {...register('society')} />
          <Input label="Area / locality" maxLength={120} placeholder="e.g. Kothrud" {...register('area')} />
          <Input label="Landmark (optional)" maxLength={200} placeholder="Near the park" {...register('landmark')} />
          <div className="space-y-2">
            <p className="text-sm text-ink-500">Pin your home on the map for accurate delivery location. Your pin is not shown to other customers; map tiles are provided by OpenStreetMap.</p>
            <Button type="button" variant="secondary" loading={locating} onClick={useCurrentLocation}>Use my current location</Button>
            {locationError && <p role="alert" className="text-sm text-red-600">{locationError}</p>}
          </div>
          <Suspense fallback={<LoadingState label="Loading map…" />}>
            <KitchenLocationPicker
              latitude={latitude}
              longitude={longitude}
              onSelect={setDeliveryLocation}
              title="Pin your home"
              instruction="Click the map or drag the pin to set your home location."
            />
          </Suspense>
          <FormError error={save.error} />
          {save.isSuccess && <p role="status" className="text-sm font-bold text-leaf-600">Saved ✓</p>}
          <Button type="submit" loading={save.isPending}>Save changes</Button>
        </form>
      </Card>
    </div>
  )
}

export function Favorites() {
  const favs = useQuery({ queryKey: ['favorites'], queryFn: () => api.get<CookSummary[]>('/favorites') })
  if (favs.isLoading) return <LoadingState />
  if (favs.error) return <ErrorState error={favs.error} onRetry={favs.refetch} />
  return (
    <div className="container-page py-8">
      <PageTitle title="Favourite cooks" />
      {!favs.data?.length ? <EmptyState emoji="♡" title="No favourites yet" message="Tap the heart on a cook's page to save them here." action={<Link to="/cooks" className="font-bold text-brand-600">Browse cooks</Link>} /> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{favs.data.map((c) => <CookCard key={c.id} cook={c} />)}</div>
      )}
    </div>
  )
}
